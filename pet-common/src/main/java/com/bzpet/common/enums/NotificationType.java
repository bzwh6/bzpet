package com.bzpet.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NotificationType {
    NOTIFICATION_TYPE_ADOPTION(0,"领养申请"),
    NOTIFICATION_TYPE_ADOPTED(1,"审核通过"),
    NOTIFICATION_TYPE_SYSTEM(2,"系统通知");
    @EnumValue
    private final int value;
    private final String desc;

    NotificationType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static NotificationType fromCode(int code) {
        for (NotificationType a : values()) {
            if (a.value == code) return a;
        }
        throw new IllegalArgumentException("未知通知类型代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }
}
