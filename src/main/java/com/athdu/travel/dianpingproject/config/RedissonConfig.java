package com.athdu.travel.dianpingproject.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 16:22
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.31.143:6379");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
//    @Bean
//    public RedissonClient redissonClient1(){
//        // 配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://192.168.31.143:7002");
//        // 创建RedissonClient对象
//        return Redisson.create(config);
//    }
//    @Bean
//    public RedissonClient redissonClient2(){
//        // 配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://192.168.31.143:7003");
//        // 创建RedissonClient对象
//        return Redisson.create(config);
//    }
}
