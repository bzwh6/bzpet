package com.bzpet.petpet.controller;


import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.domain.vo.NotificationVO;
import com.bzpet.petpet.service.INotificationService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Schema(description = "分页查询我的通知列表")
    @GetMapping("/list")
    public R<PageDTO<NotificationQueryVO>> queryList(NotificationQueryDTO dto) {
        return notificationService.queryList(dto);
    }

    @Schema(description = "查询我的通知详情")
    @GetMapping("/detail/{id}")
    public R<NotificationVO> queryDetail(@PathVariable Long id) {
        return notificationService.queryDetail(id);
    }

    @PostMapping("/read")
    public R<Void> markAllRead() {
        return notificationService.markAllRead();
    }

    @DeleteMapping("/read/clear")
    public R<Void> deleteRead() {
        return notificationService.deleteRead();
    }

    @DeleteMapping("/delete/{id}")
    public R<Void> deleteNotification(@PathVariable Long id) {
        return notificationService.deleteNotification(id);
    }

}
