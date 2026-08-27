package com.bzpet.petpet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.bzpet.petpet.mapper")
@EnableScheduling
public class PetPetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetPetApplication.class, args);
    }

}