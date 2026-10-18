package com.bzpet.petpet.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BusinessType {
    ADOPTION_APPROVE(0,"领养审批"),
    ADOPTION_APPLY(1,"领养申请");
    @EnumValue
    private final int value;
    private final String desc;

    BusinessType(int value, String desc) {

        this.value = value;
        this.desc = desc;
    }
    @JsonCreator
    public static BusinessType fromCode(int code) {
        for (BusinessType g : values()) {
            if (g.value == code) return g;
        }
        throw new IllegalArgumentException("未知业务类型代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }
}
