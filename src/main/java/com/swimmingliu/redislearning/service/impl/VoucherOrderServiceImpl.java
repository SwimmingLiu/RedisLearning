package com.swimmingliu.redislearning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.swimmingliu.redislearning.context.UserHolder;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.SeckillVoucher;
import com.swimmingliu.redislearning.entity.Voucher;
import com.swimmingliu.redislearning.entity.VoucherOrder;
import com.swimmingliu.redislearning.mapper.SeckillVoucherMapper;
import com.swimmingliu.redislearning.mapper.VoucherOrderMapper;
import com.swimmingliu.redislearning.service.ISeckillVoucherService;
import com.swimmingliu.redislearning.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.service.IVoucherService;
import com.swimmingliu.redislearning.utils.RedisWorker;
import com.swimmingliu.redislearning.utils.SimpleRedisLock;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.swimmingliu.redislearning.constant.MessageConstants.*;
import static com.swimmingliu.redislearning.constant.RedisConstants.SECKILL_STOCK_KEY;
import static com.swimmingliu.redislearning.constant.RedisConstants.VOUCHER_ORDER_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    /**
     * 秒杀下单
     *
     * @param voucherId
     * @return
     */
    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisWorker redisWorker;
    @Autowired
    private VoucherServiceImpl voucherServiceImpl;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 1. 判断优惠券是否存在
        if (seckillVoucher == null) {
            return Result.fail(VOUCHER_NOT_FOUND);
        }
        // 2. 判断秒杀活动是否在有效期
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail(VOUCHER_ACTIVITY_NOT_BEGIN);
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail(VOUCHER_ACTIVITY_ALREADY_END);
        }
        // 3. 判断库存是否充足
        if (seckillVoucher.getStock() <= 0) {
            return Result.fail(VOUCHER_STOCK_NOT_ENOUGH);
        }
        // 4. 判断是否出现一人一单
        Long userId = UserHolder.getUser().getId();
        String lockname = "order:" + userId;
        SimpleRedisLock redisLock = new SimpleRedisLock(lockname, stringRedisTemplate);
        boolean lockSuccess = redisLock.tryLock(1200);
        if (!lockSuccess) {
            return Result.fail(REPEAT_BUY_SECKILLVOUCHER_NOT_ALLOWED);
        }
        try {
            // 判断是否从夫下单
            LambdaQueryWrapper<VoucherOrder> voucherOrderWrapper = new LambdaQueryWrapper<>();
            voucherOrderWrapper.eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getVoucherId, voucherId);
            long orderCount = count(voucherOrderWrapper);
            if (orderCount > 0) {
                // 用户已经购买过了
                return Result.fail(REPEAT_BUY_SECKILLVOUCHER_NOT_ALLOWED);
            }
            // 5. 扣减库存
            LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
            // 乐观锁: 库存 > 0
            wrapper.eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId())
                    .gt(SeckillVoucher::getStock, 0)
                    .setSql("stock = stock - 1");
            boolean isReduceStockSuccess = seckillVoucherService.update(wrapper);
            if (!isReduceStockSuccess) {
                return Result.fail(REDUCE_STOCK_FAILED);
            }
            // 6. 创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            // 用户ID
            voucherOrder.setUserId(userId);
            // 优惠券ID
            voucherOrder.setVoucherId(voucherId);
            // 订单ID
            Long orderId = redisWorker.nextId(VOUCHER_ORDER_KEY);
            voucherOrder.setId(orderId);
            save(voucherOrder);
            return Result.ok(orderId);
        } finally {
            redisLock.unlock(); // 无论是否下单成功，最后都需要释放锁
        }
    }

    /**
     * JVM中解决一人一单问题 （单体项目）
     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        // 1. 判断优惠券是否存在
//        if (seckillVoucher == null) {
//            return Result.fail(VOUCHER_NOT_FOUND);
//        }
//        // 2. 判断秒杀活动是否在有效期
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail(VOUCHER_ACTIVITY_NOT_BEGIN);
//        }
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail(VOUCHER_ACTIVITY_ALREADY_END);
//        }
//        // 3. 判断库存是否充足
//        if (seckillVoucher.getStock() <= 0) {
//            return Result.fail(VOUCHER_STOCK_NOT_ENOUGH);
//        }
//        Long userId = UserHolder.getUser().getId();
//        // 用JVM当中的每一个UserID来控制锁
//        synchronized (userId.toString().intern()){
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId, seckillVoucher);
//        }
//
//    }
//    @Transactional
//    public Result createVoucherOrder(Long voucherId, SeckillVoucher seckillVoucher) {
//        // 4. 判断是否出现一人一单
//        Long userId = UserHolder.getUser().getId();
//        LambdaQueryWrapper<VoucherOrder> voucherOrderWrapper = new LambdaQueryWrapper<>();
//        voucherOrderWrapper.eq(VoucherOrder::getUserId, userId)
//                .eq(VoucherOrder::getVoucherId, voucherId);
//        long orderCount = count(voucherOrderWrapper);
//        if (orderCount > 0) {
//            return Result.fail(REPEAT_BUY_SECKILLVOUCHER_NOT_ALLOWED);
//        }
//        // 5. 扣减库存
//        LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
//        // 乐观锁: 库存 > 0
//        wrapper.eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId())
//                .gt(SeckillVoucher::getStock, 0)
//                .setSql("stock = stock - 1");
//        boolean isReduceStockSuccess = seckillVoucherService.update(wrapper);
//        if (!isReduceStockSuccess) {
//            return Result.fail(REDUCE_STOCK_FAILED);
//        }
//        // 6. 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 用户ID
//        voucherOrder.setUserId(userId);
//        // 优惠券ID
//        voucherOrder.setVoucherId(voucherId);
//        // 订单ID
//        Long orderId = redisWorker.nextId(VOUCHER_ORDER_KEY);
//        voucherOrder.setId(orderId);
//        save(voucherOrder);
//        return Result.ok(orderId);
//    }
}
