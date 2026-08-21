package com.bzpet.petuser.service;

import com.bzpet.common.domain.R;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
import com.bzpet.petuser.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-20
 */
public interface IUserService extends IService<User> {

    R<String> logIn(LoginFormDTO loginFormDTO);

    R<String> register(@Valid RegisterDTO registerDTO);
}
