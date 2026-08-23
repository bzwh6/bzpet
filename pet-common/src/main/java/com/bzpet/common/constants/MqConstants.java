package com.bzpet.common.constants;

public interface MqConstants {
    interface Exchange {
        //宠物领养相关交换机
        String PET_ADPTION_EXCHANGE = "pet_adoption_exchange";
    }

    interface Key {
        //宠物领养相关路由键
        String PET_ADPTION_ROUTING_KEY = "pet_adoption_routing_key";
    }
}
