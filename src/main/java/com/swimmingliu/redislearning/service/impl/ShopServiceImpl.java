package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.mapper.ShopMapper;
import com.swimmingliu.redislearning.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.RedisConstants.CACHE_SHOP_KEY;
import static com.swimmingliu.redislearning.constant.RedisConstants.CACHE_SHOP_TTL;

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

    /**
     * Redis 缓存查询
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 1. 查看缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2. 若存在，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 3. 不存在，查询数据库
        Shop shop = getById(id);
        if (shop == null) {
             stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",
                     CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        // 4. 添加缓存
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,
                JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺ID不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
