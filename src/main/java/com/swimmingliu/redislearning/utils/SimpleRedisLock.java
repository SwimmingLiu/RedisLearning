package com.swimmingliu.redislearning.utils;


import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private final StringRedisTemplate stringRedisTemplate;
    private final String lockname;
    private static final String KEY_PREFIX = "lock:";
    private static final String THREAD_SIGN_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // 设置lua文件位置
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        // 设置结果类型
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String lockname, StringRedisTemplate stringRedisTemplate) {
        this.lockname = lockname;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeout) {
        // 获取当前线程标识
        String threadSign = THREAD_SIGN_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + lockname, threadSign,
                timeout, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unlock() {
//        String threadSign = THREAD_SIGN_PREFIX + Thread.currentThread().getId();
//        String threadSignFromRedis = stringRedisTemplate.opsForValue().get(KEY_PREFIX + lockname);
//        if (threadSign.equals(threadSignFromRedis)){
//            stringRedisTemplate.delete(KEY_PREFIX + lockname);
//        }
        // 使用lua脚本释放锁，确保原子性
        // 获取当前线程标识
        String threadSign = THREAD_SIGN_PREFIX + Thread.currentThread().getId();
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + lockname),
                threadSign);
    }
}
