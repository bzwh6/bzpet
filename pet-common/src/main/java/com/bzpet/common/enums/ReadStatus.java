package com.bzpet.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReadStatus {
    UNREAD(0,"未读"),
    READ(1,"已读");
    @EnumValue
    private final int value;
    private final String desc;

    ReadStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static ReadStatus fromCode(int code) {
        for (ReadStatus a : values()) {
            if (a.value == code) return a;
        }
        throw new IllegalArgumentException("未知读取状态代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }
}
