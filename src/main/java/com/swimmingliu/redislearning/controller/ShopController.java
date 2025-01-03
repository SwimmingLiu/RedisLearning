package com.swimmingliu.redislearning.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Shop;
import com.swimmingliu.redislearning.service.IShopService;
import com.swimmingliu.redislearning.constant.SystemConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@RestController
@RequestMapping("/shop")
@Tag(name = "店铺信息管理")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Operation(summary = "根据id查询商铺信息")
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) throws InterruptedException {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @Operation(summary = "新增商铺信息")
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @Operation(summary = "更新商铺信息")
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        return shopService.updateShop(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @Operation(summary = "根据商铺类型分页查询商铺信息")
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y",required = false) Double y
    ) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @Operation(summary = "根据商铺名称关键字分页查询商铺信息")
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
