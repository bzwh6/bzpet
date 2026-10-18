package com.bzpet.petpet.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 宠物品种。顺序按 猫 → 狗 → 兔 → 其他 排列，前端下拉直接按枚举顺序展示。
 * <p>
 * 新增品种：在对应分组末尾追加一个枚举值并取未占用的 code，同时确保库里没有旧数据引用该 code。
 */
@Getter
public enum PetBreed {
    // ===== 猫 =====
    CHINESE_CAT(1, "中华田园猫"),
    LIHUA_CAT(2, "狸花猫"),
    ORANGE_CAT(3, "橘猫"),
    BRITISH_SHORTHAIR(4, "英短"),
    AMERICAN_SHORTHAIR(5, "美短"),
    RAGDOLL(6, "布偶猫"),
    SIAMESE(7, "暹罗猫"),
    MAINE_COON(8, "缅因猫"),
    EXOTIC_SHORTHAIR(9, "加菲猫"),
    SPHYNX(10, "无毛猫"),

    // ===== 狗 =====
    CHINESE_DOG(11, "中华田园犬"),
    GOLDEN_RETRIEVER(12, "金毛"),
    LABRADOR(13, "拉布拉多"),
    CORGI(14, "柯基"),
    SHIBA_INU(15, "柴犬"),
    HUSKY(16, "哈士奇"),
    POODLE(17, "泰迪"),
    BICHON(18, "比熊"),
    BORDER_COLLIE(19, "边牧"),
    SAMOYED(20, "萨摩耶"),
    POMERANIAN(21, "博美"),
    FRENCH_BULLDOG(22, "法斗"),

    // ===== 兔 =====
    LOP_RABBIT(23, "垂耳兔"),
    DWARF_RABBIT(24, "侏儒兔"),

    // ===== 其他 =====
    OTHER(99, "其他");

    @EnumValue
    private final int value;
    private final String desc;

    PetBreed(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static PetBreed fromCode(int code) {
        for (PetBreed b : values()) {
            if (b.value == code) return b;
        }
        throw new IllegalArgumentException("未知宠物品种代码: " + code);
    }

    @JsonValue // 序列化时返回 value
    public int getValue() { return value; }

}
