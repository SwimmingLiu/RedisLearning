package com.swimmingliu.redislearning.service;

import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
