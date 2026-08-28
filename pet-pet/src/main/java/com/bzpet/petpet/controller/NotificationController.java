package com.bzpet.petpet.controller;


import com.bzpet.common.domain.R;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.service.INotificationService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 站内通知表 前端控制器
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Schema(description = "通知相关接口")
public class NotificationController {
    private final INotificationService notificationService;

    @Schema(description = "分页查询通知列表")
    @GetMapping("/list")
    public R<List<NotificationQueryVO>> queryList(NotificationQueryDTO dto) {
        return notificationService.queryList(dto);
    }


}
