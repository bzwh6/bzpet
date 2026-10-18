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
import com.bzpet.common.domain.query.PageQuery;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.PetDTO;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.po.UserLike;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.BreedVO;
import com.bzpet.petpet.domain.vo.PetQueryVO;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.PetBreed;
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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        //公开列表：不限发布人
        return R.ok(toPageDTO(pageQuery(petDTO, null)));
    }

    @Override
    public R<PageDTO<PetQueryVO>> myPetList(PetQueryDTO petDTO) {
        //当前登录用户（网关校验登录态后通过请求头透传 userId）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BadException("请先登录");
        }
        //只查自己发布的宠物，其余筛选条件（品种/状态/年龄/性别）与列表页保持一致
        return R.ok(toPageDTO(pageQuery(petDTO, userId)));
    }

    @Override
    public R<PageDTO<PetQueryVO>> likedPetList(PageQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BadException("请先登录");
        }
        //点赞是"先写 Redis，再由定时任务落库"的，所以以 Redis 的用户点赞索引为准，
        //否则刚点的赞要等下一轮对账才会出现在列表里
        Map<Long, Long> petTimeMap = new LinkedHashMap<>();
        boolean redisIndexReady = false;
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(RedisConstants.KEY.LIKE_USER_KEY + userId, 0, -1);
            if (tuples != null) {
                for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                    if (tuple.getValue() == null) {
                        continue;
                    }
                    petTimeMap.put(Long.valueOf(tuple.getValue()),
                            tuple.getScore() == null ? 0L : tuple.getScore().longValue());
                }
            }
            redisIndexReady = true;
        } catch (Exception e) {
            log.warn("读取用户点赞索引失败，降级查询 DB：{}", e.getMessage());
        }
        //索引上线之前的历史点赞只存在于 user_like，合并进来；已存在的不覆盖 Redis 里的时间戳
        try {
            List<UserLike> likes = userLikeService.lambdaQuery()
                    .eq(UserLike::getUserId, userId)
                    .list();
            for (UserLike like : likes) {
                if (like.getPetId() == null || petTimeMap.containsKey(like.getPetId())) {
                    continue;
                }
                //索引正常时，DB 里可能还留着"已取消但没对账"的记录，用 Redis 集合再确认一次
                boolean stillLiked = true;
                if (redisIndexReady) {
                    try {
                        stillLiked = Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                                .isMember(RedisConstants.KEY.LIKE_PET_KEY + like.getPetId(),
                                        userId.toString()));
                    } catch (Exception ignored) {
                        //Redis 抖动时按 DB 记录处理，宁可多一条也不要少一条
                    }
                }
                if (stillLiked) {
                    petTimeMap.put(like.getPetId(), toEpochMilli(like.getCreateTime()));
                }
            }
        } catch (Exception e) {
            log.warn("补充历史点赞记录失败：{}", e.getMessage());
        }
        if (petTimeMap.isEmpty()) {
            return R.ok(emptyPage());
        }
        //按点赞时间倒序：Redis 的 score 是点赞那一刻，比 DB 的 create_time 更准
        List<Long> petIds = petTimeMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        //宠物已被删除的记录直接跳过
        Map<Long, Pet> petMap = listByIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getId, pet -> pet, (a, b) -> a));
        List<Pet> ordered = petIds.stream()
                .map(petMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //单个用户的点赞量有限，这里在内存里分页；若日后点赞量级变大，应改成按 petId 关联分页
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        int from = Math.min((pageNo - 1) * pageSize, ordered.size());
        int to = Math.min(from + pageSize, ordered.size());

        PageDTO<PetQueryVO> result = new PageDTO<>();
        result.setTotal((long) ordered.size());
        result.setPages((long) ((ordered.size() + pageSize - 1) / pageSize));
        if (from >= to) {
            result.setList(Collections.emptyList());
            return R.ok(result);
        }
        List<PetQueryVO> voList = ordered.subList(from, to).stream()
                .map(pet -> BeanUtil.copyProperties(pet, PetQueryVO.class))
                .collect(Collectors.toList());
        fillLikesCount(voList);
        result.setList(voList);
        return R.ok(result);
    }

    /** LocalDateTime -> 毫秒时间戳，空值按 0 处理（历史降级写入的记录没有 create_time） */
    private long toEpochMilli(LocalDateTime time) {
        return time == null ? 0L : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private PageDTO<PetQueryVO> emptyPage() {
        PageDTO<PetQueryVO> empty = new PageDTO<>();
        empty.setTotal(0L);
        empty.setPages(0L);
        empty.setList(Collections.emptyList());
        return empty;
    }

    /** 分页查询：publisherId 为 null 表示不限发布人，其余条件为 null 则不参与过滤 */
    private Page<Pet> pageQuery(PetQueryDTO dto, Long publisherId) {
        return lambdaQuery()
                .eq(publisherId != null, Pet::getPublisherId, publisherId)
                .eq(dto.getAge() != null, Pet::getAge, dto.getAge())
                .eq(dto.getGender() != null, Pet::getGender, dto.getGender())
                .eq(dto.getAdoptionStatus() != null, Pet::getStatus, dto.getAdoptionStatus())
                .eq(dto.getBreed() != null, Pet::getBreed, dto.getBreed())
                .orderByDesc(Pet::getCreateTime)
                .page(new Page<>(dto.getPageNo(), dto.getPageSize()));
    }

    /** Page&lt;Pet&gt; 转 PageDTO&lt;PetQueryVO&gt;，点赞数用 Redis 实时值覆盖（DB 的 likes_count 只在定时对账时更新） */
    private PageDTO<PetQueryVO> toPageDTO(Page<Pet> page) {
        PageDTO<PetQueryVO> result = new PageDTO<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        List<Pet> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return result;
        }
        List<PetQueryVO> voList = records.stream()
                .map(pet -> BeanUtil.copyProperties(pet, PetQueryVO.class))
                .collect(Collectors.toList());
        fillLikesCount(voList);
        result.setList(voList);
        return result;
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

    @Override
    public R<List<BreedVO>> breedList() {
        //字典来自枚举本身，顺序即枚举声明顺序（猫→狗→兔→其他）
        List<BreedVO> list = Arrays.stream(PetBreed.values())
                .map(b -> new BreedVO(b.getValue(), b.getDesc()))
                .collect(Collectors.toList());
        return R.ok(list);
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
        //点赞数不接收前端传值：插入时不写该列，由数据库默认值 0 提供，避免发布时伪造点赞量
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
            //用户维度的点赞索引：供"我点赞的"直接读 Redis，不必等定时任务落库
            String userLikeKey = RedisConstants.KEY.LIKE_USER_KEY + userId;
            List<String> list = Arrays.asList(likeKey, countKey,
                    RedisConstants.KEY.LIKE_DIRTY_PETS_KEY, userLikeKey);
            Long result = stringRedisTemplate.execute(
                    likePetScript,
                    list,
                    userId.toString(),
                    id.toString(),
                    String.valueOf(System.currentTimeMillis())
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
        String[] claimed = dirtyPets.toArray(new String[0]);
        stringRedisTemplate.opsForSet().remove(RedisConstants.KEY.LIKE_DIRTY_PETS_KEY, claimed);

        try {
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
        } catch (Exception e) {
            //补偿：把本轮认领的脏标记还回去，避免这批变更要等到下次有人点赞才重新进队列。
            //本方法以 Redis 快照为准重算差异，重跑只会得到"无差异"，不会重复插入/删除，
            //所以已写入的部分不需要回滚，只需归还标记。
            try {
                stringRedisTemplate.opsForSet().add(RedisConstants.KEY.LIKE_DIRTY_PETS_KEY, claimed);
                log.warn("点赞数据同步失败，已归还本轮脏标记，下一轮重试：{}", dirtyPets, e);
            } catch (Exception restoreEx) {
                //归还动作本身失败（多半是 Redis 不可用），只记录丢失范围，不覆盖原始异常
                log.error("归还点赞脏标记失败，这批宠物可能漏同步：{}", dirtyPets, restoreEx);
            }
            throw e;
        }
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
