package com.bzpet.petuser.service;

import com.bzpet.common.domain.R;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
import com.bzpet.petuser.domain.dto.UserInfoDTO;
import com.bzpet.petuser.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bzpet.petuser.domain.vo.UserInfoVO;
import com.bzpet.petuser.domain.vo.UserVO;
import jakarta.validation.Valid;

import java.util.List;

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

    R<UserInfoVO> userInfo(Long id);

    R<List<UserVO>> userInfoList(List<Long> ids);

    R<String> logOut(String token);

    R<String> updateUserInfo(@Valid UserInfoDTO userInfoDTO);

    R<String> updatePassword(String password);
}
