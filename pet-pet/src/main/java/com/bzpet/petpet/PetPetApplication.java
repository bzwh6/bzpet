package com.bzpet.petpet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bzpet.petpet.mapper")
public class PetPetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetPetApplication.class, args);
    }

}
