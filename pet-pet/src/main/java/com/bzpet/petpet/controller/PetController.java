package com.bzpet.petpet.controller;


import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 宠物表 前端控制器
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@RestController
@RequestMapping("/pet")
@Schema(description="宠物微服务接口")
public class PetController {

}
