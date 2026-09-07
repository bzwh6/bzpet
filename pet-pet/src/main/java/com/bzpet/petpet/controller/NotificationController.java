package com.bzpet.petpet.controller;


import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.domain.vo.NotificationVO;
import com.bzpet.petpet.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "通知相关接口")
public class NotificationController {
    private final INotificationService notificationService;

    @Operation(summary = "分页查询我的通知列表")
    @GetMapping("/list")
    public R<PageDTO<NotificationQueryVO>> queryList(@ParameterObject NotificationQueryDTO dto) {
        return notificationService.queryList(dto);
    }

    @Operation(summary = "查询我的通知详情")
    @GetMapping("/detail/{id}")
    public R<NotificationVO> queryDetail(@PathVariable Long id) {
        return notificationService.queryDetail(id);
    }

    @Operation(summary = "标记所有通知为已读")
    @PostMapping("/read")
    public R<Void> markAllRead() {
        return notificationService.markAllRead();
    }

    @Operation(summary = "删除已读通知")
    @DeleteMapping("/read/clear")
    public R<Void> deleteRead() {
        return notificationService.deleteRead();
    }

    @Operation(summary = "删除指定通知")
    @DeleteMapping("/delete/{id}")
    public R<Void> deleteNotification(@PathVariable Long id) {
        return notificationService.deleteNotification(id);
    }

    @Operation(summary = "查询我的未读通知数量")

    @GetMapping("/unread/count")
    public R<Long> queryUnreadCount() {
        return notificationService.queryUnreadCount();
    }
}
