package com.bzpet.petuser.service.impl;

import com.bzpet.petuser.domain.po.User;
import com.bzpet.petuser.mapper.UserMapper;
import com.bzpet.petuser.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-20
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
