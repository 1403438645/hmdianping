package com.athdu.travel.dianpingproject;

import io.lettuce.core.ReadFrom;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication

@MapperScan("com.athdu.travel.dianpingproject.mapper")
public class DianpingProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianpingProjectApplication.class, args);
    }



}
