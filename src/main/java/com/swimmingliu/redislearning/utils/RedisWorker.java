package com.swimmingliu.redislearning.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 生成全局唯一ID
 */
@Component
public class RedisWorker {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP = 1726876800L; // 开始时间：2024-9-21 00:00:00
    private static final int COUNT_BITS = 32; // 序列号的位数
    private static final String KEY_PREFIX = "icr:";
    public Long nextId(String keyPrefix){
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long nowTimestamp = nowEpochSecond - BEGIN_TIMESTAMP;
        // 2. 生成序列号
        // 2.1 利用当前时间作为key进行区分 （按天）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String key = KEY_PREFIX + keyPrefix + ":" + date;
        // 2.2 自增长
        long count = stringRedisTemplate.opsForValue().increment(key);
        // 3. 拼接最终结果
        return nowTimestamp << COUNT_BITS | count;
    }
}
