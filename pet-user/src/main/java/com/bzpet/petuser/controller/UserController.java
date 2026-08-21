package com.bzpet.petuser.controller;


import com.bzpet.common.domain.R;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
import com.bzpet.petuser.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
    @PostMapping("/register")
    @Operation(summary = "注册接口")
    public R<String> register(@Valid @RequestBody RegisterDTO registerDTO){
        return userService.register(registerDTO);
    }
}
