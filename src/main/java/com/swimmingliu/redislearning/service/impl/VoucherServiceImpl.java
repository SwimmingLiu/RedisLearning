package com.swimmingliu.redislearning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Voucher;
import com.swimmingliu.redislearning.mapper.VoucherMapper;
import com.swimmingliu.redislearning.entity.SeckillVoucher;
import com.swimmingliu.redislearning.service.ISeckillVoucherService;
import com.swimmingliu.redislearning.service.IVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }
}
