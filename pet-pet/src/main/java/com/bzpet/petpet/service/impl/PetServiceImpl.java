package com.bzpet.petpet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.constants.RedisConstants;
import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.PetDTO;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.po.UserLike;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.PetQueryVO;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.mapper.PetMapper;
import com.bzpet.petpet.service.IPetService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bzpet.petpet.service.IUserLikeService;
import com.bzpet.petpet.service.UserLikeFallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 宠物表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements IPetService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final UserLikeFallbackService userLikeFallbackService;
    private final RedisScript<Long> likePetScript;
    private final IUserLikeService userLikeService;

    @Override
    public R<PageDTO<PetQueryVO>> petList(PetQueryDTO petDTO) {
        //分页查询宠物列表
        //获取分页查询参数
        Integer pageNo = petDTO.getPageNo();
        Integer pageSize = petDTO.getPageSize();
        //获取查询条件
        Integer age = petDTO.getAge();
        Integer gender = petDTO.getGender();
        Integer adoptionStatus = petDTO.getAdoptionStatus();
        //TODO 品种
        //分页查询宠物列表
        Page<Pet> page = lambdaQuery()
                .eq(age != null, Pet::getAge, age)
                .eq(gender != null, Pet::getGender, gender)
                .eq(adoptionStatus != null, Pet::getStatus, adoptionStatus)
                .orderByDesc(Pet::getCreateTime)
                .page(new Page<>(pageNo, pageSize));
        List<Pet> records = page.getRecords();

        PageDTO<PetQueryVO> result = new PageDTO<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        if (CollUtil.isEmpty(records)) {
            return R.ok(result);
        }
        // 5. 转换结果（Pet -> PetQueryVO）
        List<PetQueryVO> voList = records.stream()
                .map(pet -> BeanUtil.copyProperties(pet, PetQueryVO.class))
                .collect(Collectors.toList());
        // 6. 用 Redis 实时点赞数覆盖（DB 的 likes_count 只在定时对账时更新）
        fillLikesCount(voList);
        // 7. 封装分页结果
        result.setList(voList);
        return R.ok(result);
    }

    @Override
    public R<PetQueryVO> petDetail(Long id) throws InterruptedException {
        //通过redis查询宠物详情
        String key = RedisConstants.KEY.PET_DETAIL_KEY + id;
        String p = stringRedisTemplate.opsForValue().get(key);
        //redis里是否存在
        if (p != null && !p.isEmpty()) {
            //存在，直接返回结果
            Pet bean = JSONUtil.toBean(p, Pet.class);
            return R.ok(toPetVO(bean));
        } else if (Objects.equals(p, "")) {
            //redis里存在空值，说明宠物不存在
            return R.error("宠物不存在");
        }
        //不存在，从数据库查
        //用redisson锁，防止并发查询数据库，解决缓存击穿问题
        RLock lock = redissonClient.getLock(id.toString());
        boolean tryLock = lock.tryLock(1, 2, TimeUnit.SECONDS);
        try {
            if (!tryLock) {
                return R.error("系统繁忙，请稍后重试");
            }
            //先查缓存，看看是否重建了
            p = stringRedisTemplate.opsForValue().get(key);
            if (p != null && !p.isEmpty()) {
                //存在，直接返回结果
                Pet bean = JSONUtil.toBean(p, Pet.class);
                return R.ok(toPetVO(bean));
            }
            //不存在，从数据库查
            Pet pet = getById(id);
            if (pet == null) {
                //数据库也不存在，redis里存空值
                stringRedisTemplate.opsForValue()
                        .set(key, "", RedisConstants.EXPIRE.NULL_EXPIRE, TimeUnit.SECONDS);
                return R.error("宠物不存在");
            }
            //存在，写入redis,
            String jsonStr = JSONUtil.toJsonStr(pet);
            //0-5分钟的随机过期时间
            long randomExpire = ThreadLocalRandom.current().nextLong(0, 5 * 60);
            stringRedisTemplate.opsForValue()
                    .set(key, jsonStr, RedisConstants.EXPIRE.PET_DETAIL_EXPIRE + randomExpire,
                            TimeUnit.SECONDS);
            return R.ok(toPetVO(pet));

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tryLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 单条：用 Redis 中的实时点赞数覆盖 VO */
    private PetQueryVO toPetVO(Pet pet) {
        PetQueryVO vo = BeanUtil.copyProperties(pet, PetQueryVO.class);
        if (vo.getId() != null) {
            Double score = stringRedisTemplate.opsForZSet()
                    .score(RedisConstants.KEY.LIKE_PET_COUNT_KEY, vo.getId().toString());
            if (score != null) {
                vo.setLikesCount(score.intValue());
            }
        }
        return vo;
    }

    /** 批量：用 Redis 中的实时点赞数覆盖 VO（DB 的 likes_count 只在定时对账时更新） */
    private void fillLikesCount(List<PetQueryVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        List<PetQueryVO> valid = voList.stream()
                .filter(vo -> vo.getId() != null)
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            return;
        }
        Object[] members = valid.stream().map(vo -> vo.getId().toString()).toArray();
        List<Double> scores = stringRedisTemplate.opsForZSet()
                .score(RedisConstants.KEY.LIKE_PET_COUNT_KEY, members);
        if (scores == null) {
            return;
        }
        for (int i = 0; i < valid.size() && i < scores.size(); i++) {
            Double score = scores.get(i);
            if (score != null) {
                valid.get(i).setLikesCount(score.intValue());
            }
        }
    }

    @Override
    public void addPet(PetDTO petDTO) {
        //转换为Pet对象
        Pet pet = BeanUtil.copyProperties(petDTO, Pet.class);
        //设置发布人ID
        pet.setPublisherId(UserContext.getUserId());
        //设置状态为待领养
        pet.setStatus(AdoptionStatus.AVAILABLE);
        //保存到数据库
        save(pet);
    }

    @Override
    public void deletePet(Long id) {
        //查询宠物是否存在
        Pet pet = getById(id);
        checkPet(pet, UserContext.getUserId());
        //删除宠物缓存
        stringRedisTemplate.delete(RedisConstants.KEY.PET_DETAIL_KEY + id);
        //删除宠物,只有待领养的宠物才能删除
        if (pet.getStatus() != AdoptionStatus.AVAILABLE) {
            throw new BadException("只有待领养的宠物才能删除");
        }
        removeById(id);
    }

    @Override
    public void updatePet(Long id, PetDTO petDTO) {
        //查询宠物是否存在
        Pet pet = getById(id);
        checkPet(pet, UserContext.getUserId());
        //更新数据库
        lambdaUpdate()
                .eq(Pet::getId, id)
                .set(petDTO.getPetName() != null, Pet::getPetName, petDTO.getPetName())
                .set(petDTO.getBreed() != null, Pet::getBreed, petDTO.getBreed())
                .set(petDTO.getAge() != null, Pet::getAge, petDTO.getAge())
                .set(petDTO.getGender() != null, Pet::getGender, petDTO.getGender())
                .set(petDTO.getDescription() != null, Pet::getDescription, petDTO.getDescription())
                .set(petDTO.getImageUrl() != null, Pet::getImageUrl, petDTO.getImageUrl())
                .update();
        //删除宠物缓存
        stringRedisTemplate.delete(RedisConstants.KEY.PET_DETAIL_KEY + id);
    }


    @Override
    @SentinelResource(value = "likePetResource", blockHandler = "likeFallback")
    public void likePet(Long id) {
        //获取用户id
        Long userId = UserContext.getUserId();
        try {
            //用redis的set集合，记录点赞的用户id
            String likeKey = RedisConstants.KEY.LIKE_PET_KEY + id;
            String countKey = RedisConstants.KEY.LIKE_PET_COUNT_KEY;
            List<String> list = Arrays.asList(likeKey, countKey, RedisConstants.KEY.LIKE_DIRTY_PETS_KEY);
            Long result = stringRedisTemplate.execute(
                    likePetScript,
                    list,
                    userId.toString(),
                    id.toString()
            );
        } catch (Exception e) {
            try {
                userLikeFallbackService.likeFallback(userId, id);
            } catch (Exception ex) {
                log.error("系统繁忙，请稍后再试", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public boolean isLiked(Long petId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(RedisConstants.KEY.LIKE_PET_KEY + petId, userId.toString()));
    }

    @Override
    public void saveLikeCount() {
        //取出待同步的宠物id（点赞/取消时由 Lua 脚本标记进 dirty 集合）
        Set<String> dirtyPets = stringRedisTemplate.opsForSet()
                .members(RedisConstants.KEY.LIKE_DIRTY_PETS_KEY);
        if (CollUtil.isEmpty(dirtyPets)) {
            return;
        }
        List<Long> petIds = dirtyPets.stream().map(Long::valueOf).collect(Collectors.toList());
        log.info("开始同步点赞数据，待同步宠物数：{}", petIds.size());

        //先删脏标记（认领）再处理：处理期间若有人点赞/取消，Lua 会重新标记该宠物，
        //下一轮任务会带着最新快照再同步一次，避免并发变更的标记被误删导致永久漏同步
        stringRedisTemplate.opsForSet().remove(RedisConstants.KEY.LIKE_DIRTY_PETS_KEY,
                dirtyPets.toArray(new String[0]));

        //1. 从 Redis 取每个宠物的点赞用户集合（key 不存在视为空集，即被取消到 0 赞）
        Map<Long, Set<Long>> redisLikeMap = new HashMap<>(petIds.size());
        for (Long petId : petIds) {
            Set<String> users = stringRedisTemplate.opsForSet()
                    .members(RedisConstants.KEY.LIKE_PET_KEY + petId);
            redisLikeMap.put(petId, users == null ? Collections.<Long>emptySet()
                    : users.stream().map(Long::valueOf).collect(Collectors.toSet()));
        }

        //2. 批量查出这些宠物在 DB 的点赞记录（IN 分批，避免单条 SQL 过长）
        List<UserLike> dbLikes = new ArrayList<>();
        for (List<Long> batch : CollUtil.split(petIds, 500)) {
            dbLikes.addAll(userLikeService.lambdaQuery()
                    .in(UserLike::getPetId, batch)
                    .list());
        }
        Map<Long, List<UserLike>> dbLikesByPet = dbLikes.stream()
                .collect(Collectors.groupingBy(UserLike::getPetId));

        //3. 在内存中算出差异，收集成批量操作
        List<UserLike> toInsert = new ArrayList<>();
        List<Long> toDeleteIds = new ArrayList<>();
        List<Pet> toUpdateCount = new ArrayList<>(petIds.size());
        for (Long petId : petIds) {
            Set<Long> redisUsers = redisLikeMap.get(petId);
            List<UserLike> petDbLikes = dbLikesByPet.getOrDefault(petId, Collections.<UserLike>emptyList());
            Set<Long> dbUsers = petDbLikes.stream().map(UserLike::getUserId).collect(Collectors.toSet());

            //Redis 有、DB 无 → 新增（新点赞）
            for (Long userId : redisUsers) {
                if (!dbUsers.contains(userId)) {
                    toInsert.add(new UserLike()
                            .setUserId(userId)
                            .setPetId(petId)
                            .setCreateTime(LocalDateTime.now()));
                }
            }
            //DB 有、Redis 无 → 删除（取消点赞）
            for (UserLike dbLike : petDbLikes) {
                if (!redisUsers.contains(dbLike.getUserId())) {
                    toDeleteIds.add(dbLike.getId());
                }
            }
            //点赞总数以 Redis 集合大小为准覆盖
            toUpdateCount.add(new Pet().setId(petId).setLikesCount(redisUsers.size()));
        }

        //4. 批量落库
        if (CollUtil.isNotEmpty(toInsert)) {
            userLikeService.saveBatch(toInsert);
        }
        if (CollUtil.isNotEmpty(toDeleteIds)) {
            userLikeService.removeByIds(toDeleteIds);
        }
        if (CollUtil.isNotEmpty(toUpdateCount)) {
            updateBatchById(toUpdateCount);
        }

        //5. 落库完成（脏标记已在处理前认领删除，此处不再删除）
        log.info("点赞数据同步完成，新增{}条，删除{}条", toInsert.size(), toDeleteIds.size());
    }

    public void likeFallback(Long id, BlockException e) {
        log.warn("点赞宠物失败，id：{}", id);
        throw new BadException("系统繁忙，请稍后再试！");
    }

    private void checkPet(Pet pet, Long userId) {
        //检查发布人是否是当前用户
        //检查宠物是否存在
        if (pet == null) {
            throw new BadException("宠物不存在");
        }
        if (!Objects.equals(pet.getPublisherId(), userId)) {
            throw new BadException("只有发布人才能更新");
        }
    }
}
