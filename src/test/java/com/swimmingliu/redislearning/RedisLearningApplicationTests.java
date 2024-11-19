package com.swimmingliu.redislearning;

import com.swimmingliu.redislearning.entity.RedisData;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.service.IShopService;
import com.swimmingliu.redislearning.service.impl.ShopServiceImpl;
import com.swimmingliu.redislearning.utils.CacheCient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class RedisLearningApplicationTests {

    @Autowired
    private CacheCient cacheCient;
    @Autowired
    private IShopService shopService;
    @Test
    void contextLoads() {
    }

    @Test
    void putShopCacheWithLogicExpire(){
        Shop shop = shopService.getById(1);
        cacheCient.setWithLogicExpire(CACHE_SHOP_KEY + 1, shop, 10L, TimeUnit.SECONDS);
    }


}
