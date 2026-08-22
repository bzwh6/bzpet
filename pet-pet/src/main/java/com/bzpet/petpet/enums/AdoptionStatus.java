package com.bzpet.petpet.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AdoptionStatus {
    AVAILABLE(0,"可领养"),
    ADOPTED(1,"已领养"),
    OFF_SHELF(2,"已下架");
    @EnumValue
    private final int value;
    private final String desc;

    AdoptionStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static AdoptionStatus fromCode(int code) {
        for (AdoptionStatus a : values()) {
            if (a.value == code) return a;
        }
        throw new IllegalArgumentException("未知领养状态代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }

}
