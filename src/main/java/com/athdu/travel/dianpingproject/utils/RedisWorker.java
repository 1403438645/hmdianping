package com.athdu.travel.dianpingproject.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author baizhejun
 * @create 2022 -10 -19 - 21:58
 */
@Component
public class RedisWorker {
//    开始时间戳
    private final long BEGIN_TIMESTAMP =1640995200L ;
//    序列号的位数
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisWorker(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
//        1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

//        2.生成序列号
//        获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
//        3.redis自增
        long count = stringRedisTemplate.opsForValue().increment("icr"+keyPrefix+":"+date);
//        4.拼接并返回
        return timestamp << COUNT_BITS | count;

    }
}

