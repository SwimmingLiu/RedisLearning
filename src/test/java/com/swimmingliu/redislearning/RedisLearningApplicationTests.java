package com.swimmingliu.redislearning;

import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.service.IShopService;
import com.swimmingliu.redislearning.utils.CacheCient;
import com.swimmingliu.redislearning.utils.RedisWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class RedisLearningApplicationTests {

    @Autowired
    private CacheCient cacheCient;
    @Autowired
    private IShopService shopService;
    @Autowired
    private RedisWorker redisWorker;
    @Autowired
    private RedissonClient redissonClient;
    private final ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void putShopCacheWithLogicExpire() {
        Shop shop = shopService.getById(1);
        cacheCient.setWithLogicExpire(CACHE_SHOP_KEY + 1, shop, 10L, TimeUnit.SECONDS);
    }

    @Test
    void testIdWorker() throws InterruptedException {
        // 使用CountDownLatch 记录多线程执行后的时间，计数器
        CountDownLatch latch = new CountDownLatch(300); // 300个计数器
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisWorker.nextId("voucher:order");
                System.out.println(id);
            }
            latch.countDown();
        };
        Long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        Long endTime = System.currentTimeMillis();
        System.out.println("time = " + (endTime - beginTime));
    }

    @Test
    void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLockfortest");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (!isLock)
            return;
        try {
            System.out.println("上锁成功, 开始执行任务...");
        }finally {
            lock.unlock();
        }
    }
}
