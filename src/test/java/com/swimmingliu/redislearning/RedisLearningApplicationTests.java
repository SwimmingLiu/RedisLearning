package com.swimmingliu.redislearning;

import com.swimmingliu.redislearning.entity.RedisData;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.service.IShopService;
import com.swimmingliu.redislearning.service.impl.ShopServiceImpl;
import com.swimmingliu.redislearning.utils.CacheCient;
import com.swimmingliu.redislearning.utils.RedisWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
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
}
