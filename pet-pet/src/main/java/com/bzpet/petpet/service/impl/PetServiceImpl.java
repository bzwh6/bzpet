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
import com.bzpet.petpet.enums.ApplicaltionStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
        // 6. 封装分页结果
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
            PetQueryVO vo = BeanUtil.copyProperties(bean, PetQueryVO.class);
            return R.ok(vo);
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
                PetQueryVO vo = BeanUtil.copyProperties(bean, PetQueryVO.class);
                return R.ok(vo);
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
            return R.ok(BeanUtil.copyProperties(pet, PetQueryVO.class));

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tryLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
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
    @SentinelResource(value = "likePet", blockHandler = "likeFallback")
    public void likePet(Long id) {
        //获取用户id
        Long userId = UserContext.getUserId();
        try {
            //用redis的set集合，记录点赞的用户id
            String likeKey = RedisConstants.KEY.LIKE_PET_KEY + id;
            Long result = stringRedisTemplate.opsForSet().add(likeKey, userId.toString());
            String countKey = RedisConstants.KEY.LIKE_PET_COUNT_KEY;
            if (result != null && result == 0) {
                //删除点赞的用户id缓存
                stringRedisTemplate.opsForSet().remove(likeKey, userId.toString());
                //如果用户已经点赞说明是取消点赞,点赞总数减1
                Double v = stringRedisTemplate.opsForZSet().incrementScore(countKey, id.toString(), -1);
                //如果点赞数为负数，
                if (v != null && v < 0) {
                    //点赞数不能为负数,把点赞总数设为0
                    stringRedisTemplate.opsForZSet().add(countKey, id.toString(), 0);
                }
            } else {
                //没有就是点赞
                //用zset记录点赞数,点赞总数加1
                stringRedisTemplate.opsForZSet().incrementScore(countKey, id.toString(), 1);
            }
        } catch (Exception e) {
            try {
                userLikeFallbackService.likeFallback(userId, id);
            } catch (Exception ex) {
                log.error("系统繁忙，请稍后再试", ex);
                throw new RuntimeException(ex);
            }
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
