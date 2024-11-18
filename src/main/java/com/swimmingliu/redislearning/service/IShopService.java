package com.swimmingliu.redislearning.service;

import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result updateShop(Shop shop);
}
