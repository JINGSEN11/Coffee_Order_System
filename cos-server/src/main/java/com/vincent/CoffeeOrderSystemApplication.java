package com.vincent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vincent.mapper")
public class CoffeeOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeOrderSystemApplication.class, args);
    }

}