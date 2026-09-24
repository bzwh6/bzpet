package com.bzpet.petuser.controller;


import com.bzpet.common.domain.R;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
import com.bzpet.petuser.domain.dto.UpdatePasswordDTO;
import com.bzpet.petuser.domain.dto.UserInfoDTO;
import com.bzpet.petuser.domain.vo.UserInfoVO;
import com.bzpet.petuser.domain.vo.UserVO;
import com.bzpet.petuser.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author bzwh
 * @since 2026-08-20
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户相关接口")
public class UserController {
    @Autowired
    private IUserService userService;

    @PostMapping("/login")
    @Operation(summary = "登录接口")
    public R<String> logIn(@Valid @RequestBody LoginFormDTO loginFormDTO){
        return userService.logIn(loginFormDTO);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录接口")
    public R<String> logOut(String token){
        return userService.logOut(token);
    }

    @PostMapping("/register")
    @Operation(summary = "注册接口")
    public R<String> register(@Valid @RequestBody RegisterDTO registerDTO){
        return userService.register(registerDTO);
    }

    @GetMapping("/userInfo/{id}")
    @Operation(summary = "用户信息接口")
    public R<UserInfoVO> userInfo(@PathVariable Long id){
        return userService.userInfo(id);
    }

    @GetMapping("/list")
    @Operation(summary = "用户列表接口")
    public R<List<UserVO>> userInfoList(@RequestParam List<Long> ids){
        return userService.userInfoList(ids);
    }

    @PutMapping("/update/info")
    @Operation(summary = "更新用户基础信息接口")
    public R<String> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO){
        return userService.updateUserInfo(userInfoDTO);
    }

    @PutMapping("/update/password")
    @Operation(summary = "更新用户密码接口")
    public R<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO){
        return userService.updatePassword(updatePasswordDTO);
    }
}
