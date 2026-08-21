package com.bzpet.petuser;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bzpet.petuser.mapper")
public class PetUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetUserApplication.class, args);
    }

}
