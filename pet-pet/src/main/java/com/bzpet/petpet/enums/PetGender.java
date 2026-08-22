package com.bzpet.petpet.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PetGender {
    Male(0,"公"),
    Female(1,"母");
    @EnumValue
    private final int value;
    private final String desc;

    PetGender(int value, String desc) {

        this.value = value;
        this.desc = desc;
    }
    @JsonCreator
    public static PetGender fromCode(int code) {
        for (PetGender g : values()) {
            if (g.value == code) return g;
        }
        throw new IllegalArgumentException("未知性别代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }

}
