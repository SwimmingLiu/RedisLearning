package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swimmingliu.redislearning.constant.SystemConstants;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.mapper.ShopMapper;
import com.swimmingliu.redislearning.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.utils.CacheCient;
import jakarta.annotation.Resource;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1. 判断是否调用坐标系
        if (x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2. 计算相应的页码
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3. 查询当前类型对应的附近商户
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = stringRedisTemplate.opsForGeo()
                .search(key, GeoReference.fromCoordinate(new Point(x, y)),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .includeDistance()
                                .limit(end));
        if (geoResults == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> geoList = geoResults.getContent();
        if (geoList.size() <= start) {
            // 没有下一页
            return Result.ok(Collections.emptyList());
        }
        // 4. 截取[start, end]之间的数据
        List<String> ids = new ArrayList<>(geoList.size());
        HashMap<String, Distance> distanceMap = new HashMap<>(geoList.size());
        geoList.stream().skip(start).forEach(geo -> {
            String id = geo.getContent().getName();
            ids.add(id);
            distanceMap.put(id, geo.getDistance());
        });
        // 5.获取ids对应的shop信息
        String idsStr = StrUtil.join(",", ids);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .in(Shop::getId, ids)
                .last("order by field(id, " + idsStr + ")");
        List<Shop> shopList = list(wrapper);
        shopList.forEach(shop -> shop.setDistance(distanceMap.get(shop.getId().toString()).getValue()));
        return Result.ok(shopList);
    }
}
