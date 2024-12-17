package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.mapper.ShopMapper;
import com.swimmingliu.redislearning.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.utils.CacheCient;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheCient cacheCient;

    /**
     * Redis 缓存查询
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) throws InterruptedException {
//        // 解决缓存穿透
//        Shop shop = cacheCient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById,
//                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 解决缓存击穿 - 互斥锁
//        Shop shop = cacheCient.queryWithMutexLock(CACHE_SHOP_KEY, id, Shop.class, this::getById,
//                CACHE_SHOP_TTL, TimeUnit.MINUTES, LOCK_SHOP_KEY, LOCK_SHOP_TTL);
        // 解决缓存击穿 - 逻辑过期
        Shop shop = cacheCient.queryWithLogicExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById,
                CACHE_SHOP_TTL, TimeUnit.MINUTES, LOCK_SHOP_KEY, LOCK_SHOP_TTL);
        if (shop != null) {
            return Result.ok(shop);
        }
        // 缓存第一次不存在，查询数据库
        shop = getById(id);
        if (shop == null){
             return Result.fail("店铺不存在");
        }
        return  Result.ok(shop);
    }

    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺ID不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
