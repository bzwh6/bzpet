package com.bzpet.petpet.service.impl;

import com.bzpet.petpet.domain.po.UserLike;
import com.bzpet.petpet.mapper.UserLikeMapper;
import com.bzpet.petpet.service.IUserLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 宠物点赞记录表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-09-08
 */
@Service
public class UserLikeServiceImpl extends ServiceImpl<UserLikeMapper, UserLike> implements IUserLikeService {

}
