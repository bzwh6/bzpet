package com.bzpet.petpet.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ApplicaltionStatus {
    PENDING(0,"待审核"),
    APPROVED(1,"已同意"),
    REJECTED(2,"已拒绝");
    @EnumValue
    private final int value;
    private final String desc;

    ApplicaltionStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static ApplicaltionStatus fromCode(int code) {
        for (ApplicaltionStatus a : values()) {
            if (a.value == code) return a;
        }
        throw new IllegalArgumentException("未知领养申请状态代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }

}
