package com.bzpet.petuser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.bzpet.common.domain.R;
import com.bzpet.common.exception.BadException;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
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

    @Override
    public R<String> logIn(LoginFormDTO loginFormDTO) {
        //从数据库里根据用户名查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, loginFormDTO.getUsername())
                .eq(User::getPassword, loginFormDTO.getPassword())
                .one();
        if (user == null){
            throw new BadException("用户名或密码错误！");
        }
        return R.ok("登录成功！");
    }

    @Override
    public R<String> register(RegisterDTO registerDTO) {
        //直接插入
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        //如果有一样的用户名会报异常
        try {
            save(user);
        } catch (Exception e) {
            throw new BadException("用户名已存在！");
        }
        return R.ok("注册成功。");
    }
}
