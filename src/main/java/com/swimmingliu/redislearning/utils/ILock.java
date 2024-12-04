package com.swimmingliu.redislearning.utils;

public interface ILock {
    /**
     * 获取锁
     * @param timeout
     * @return
     */
    public boolean tryLock(long timeout);

    /**
     * 释放锁
     */
    public void unlock();
}
