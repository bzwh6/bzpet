package com.bzpet.petpet.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.po.UserLike;
import com.bzpet.petpet.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLikeFallbackService {
    private final IUserLikeService userLikeService;
    private final PetMapper petMapper;

    @Transactional(rollbackFor = Exception.class)
    public void likeFallback(Long userId, Long id) {
        //缓存出异常，从数据库查
        UserLike userLike = userLikeService.lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getPetId, id)
                .one();
        if (userLike == null) {
            //没有就是点赞，直接用数据库加
            try {
                userLikeService.save(new UserLike().setUserId(userId).setPetId(id));
            } catch (Exception e) {
                log.error("点赞宠物失败", e);
            }
            petMapper.update(null, new LambdaUpdateWrapper<Pet>()
                    .eq(Pet::getId, id)
                    .setSql("likes_count = likes_count + 1")
            );
        } else {
            //有就是取消点赞,点赞总数减1
            //有就是取消点赞，直接用数据库减
            userLikeService.removeById(userLike.getId());
            petMapper.update(null, new LambdaUpdateWrapper<Pet>()
                    .eq(Pet::getId, id)
                    .setSql("likes_count = GREATEST(0, likes_count - 1)")
            );
        }
    }
}
