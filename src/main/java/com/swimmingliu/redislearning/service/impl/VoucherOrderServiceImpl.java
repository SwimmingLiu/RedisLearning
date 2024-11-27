package com.swimmingliu.redislearning.service.impl;

import com.swimmingliu.redislearning.context.UserHolder;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Voucher;
import com.swimmingliu.redislearning.entity.VoucherOrder;
import com.swimmingliu.redislearning.mapper.VoucherOrderMapper;
import com.swimmingliu.redislearning.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.service.IVoucherService;
import com.swimmingliu.redislearning.utils.RedisWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.swimmingliu.redislearning.constant.MessageConstants.*;
import static com.swimmingliu.redislearning.constant.RedisConstants.SECKILL_STOCK_KEY;
import static com.swimmingliu.redislearning.constant.RedisConstants.VOUCHER_ORDER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    /**
     * 秒杀下单
     * @param voucherId
     * @return
     */

    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private RedisWorker redisWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Voucher voucher = voucherService.getById(voucherId);
        // 1. 判断优惠券是否存在
        if (voucher == null){
            return Result.fail(VOUCHER_NOT_FOUND);
        }
        // 2. 判断秒杀活动是否在有效期
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail(VOUCHER_ACTIVITY_NOT_BEGIN);
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(VOUCHER_ACTIVITY_ALREADY_END);
        }
        // 3. 判断库存是否充足
        if (voucher.getStock() <= 0){
            return Result.fail(VOUCHER_STOCK_NOT_ENOUGH);
        }
        // 4. 扣减库存
        voucher.setStock(voucher.getStock() - 1);
        voucherService.updateById(voucher);
        // 5. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 用户ID
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 优惠券ID
        voucherOrder.setVoucherId(voucherId);
        // 订单ID
        Long orderId = redisWorker.nextId(VOUCHER_ORDER_KEY);
        voucherOrder.setVoucherId(orderId);
        save(voucherOrder);
        // TODO 测试超卖
        return Result.ok(orderId);
    }
}
