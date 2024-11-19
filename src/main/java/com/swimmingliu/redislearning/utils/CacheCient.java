package com.swimmingliu.redislearning.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.swimmingliu.redislearning.entity.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.swimmingliu.redislearning.constant.RedisConstants.*;

@Slf4j
@Component
public class CacheCient {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 设定Lock的值
    private final static String LOCK_VALUE = "1";
    private final static Integer SLEEP_TIME = 50;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private String getStringCache(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    private void setStringCache(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    private void setStringCache(String key, String value, Long ttl, TimeUnit timeUnit) {
        // 解决缓存雪崩中的TTL同时过期
        ttl = RandomUtil.randomLong(1L, 10L) * ttl;
        stringRedisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    private Boolean getLock(String key, Long ttl) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, LOCK_VALUE, ttl, TimeUnit.SECONDS);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    public void setWithLogicExpire(String key, Object data, Long time, TimeUnit timeUnit) {
        RedisData redisData = RedisData.builder()
                .data(data)
                .ttl(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)))
                .build();
        this.setStringCache(key, JSONUtil.toJsonStr(redisData));
    }

    // 缓存穿透
    public <T, ID> T queryWithPassThrough(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback,
                                          Long cacheTTL, TimeUnit timeUnit) {
        // 1. 查看缓存
        String key = keyPrefix + id;
        String cacheJson = this.getStringCache(key);
        // 2. 若存在，直接返回
        if (StrUtil.isNotBlank(cacheJson)) {
            return JSONUtil.toBean(cacheJson, type);
        }
        if (cacheJson != null && cacheJson.isEmpty()) {
            return null;
        }
        // 3. 不存在，查询数据库
        T t = dbFallback.apply(id);
        if (t == null) {
            this.setStringCache(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4. 添加缓存
        this.setStringCache(key, JSONUtil.toJsonStr(t), cacheTTL, timeUnit);
        return t;
    }

    // 缓存击穿-互斥锁
    public <T, ID> T queryWithMutexLock(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback,
                                        Long cacheTTL, TimeUnit timeUnit, String lockKeyPrefix,
                                        Long lockTTL) throws InterruptedException {
        // 1. 查看缓存
        String key = keyPrefix + id;
        String cacheJson = this.getStringCache(key);
        // 2. 若存在，直接返回
        if (StrUtil.isNotBlank(cacheJson)) {
            return JSONUtil.toBean(cacheJson, type);
        }
        if (cacheJson != null && cacheJson.isEmpty()) {
            return null;
        }
        // 3. 不存在，查询数据库
        T t = null;
        String lockKey = lockKeyPrefix + id;
        // 3.1 获取锁
        try {
            Boolean isGetLock = this.getLock(lockKey, lockTTL);
            if (!isGetLock) {
                // 获取锁失败，休眠并重试
                Thread.sleep(SLEEP_TIME);
                return this.queryWithMutexLock(keyPrefix, id, type, dbFallback, cacheTTL, timeUnit,
                        lockKeyPrefix, lockTTL);
            }
            // 3.2 查询数据库
            t = dbFallback.apply(id);
            if (t == null) {
                this.setStringCache(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 3.3 添加缓存
            this.setStringCache(key, JSONUtil.toJsonStr(t), cacheTTL, timeUnit);
        }
        // 处理异常
        catch (InterruptedException err) {
            throw new RuntimeException(err);
        }
        // 3.4 释放锁
        finally {
            this.unLock(lockKey);
        }
        return t;
    }

    // 缓存击穿-逻辑过期
    public <T, ID> T queryWithLogicExpire(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback,
                                          Long cacheTTL, TimeUnit timeUnit, String lockKeyPrefix,
                                          Long lockTTL) throws InterruptedException {
        // 1. 查看缓存
        String key = keyPrefix + id;
        String cacheJson = this.getStringCache(key);
        // 2. 若不存在或为空，直接返回
        if (StrUtil.isBlank(cacheJson)) {
            return null;
        }
        // 3. 存在，判断key是否过期
        RedisData redisData = JSONUtil.toBean(cacheJson, RedisData.class);
        T t = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime redisDataTTL = redisData.getTtl();
        // 未过期
        if (redisDataTTL.isAfter(LocalDateTime.now())) {
            return t;
        }
        // 已过期
        String lockKey = lockKeyPrefix + id;
        // 3.1 获取锁
        Boolean isGetLock = this.getLock(lockKey, lockTTL);
        // 获取锁失败，返回过期数据
        if (!isGetLock) return t;
        // 3.2 获取锁成功
        // 3.3 创建线程，更新数据和过期时间
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                Thread.sleep(5000);
                // 3.2 查询数据库
                T newT = dbFallback.apply(id);
                this.setWithLogicExpire(key, newT, cacheTTL, timeUnit);
            }
            // 3.4 释放锁
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                this.unLock(lockKey);
            }
        });
        return t;
    }
}
