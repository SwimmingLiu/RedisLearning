package com.swimmingliu.redislearning.service.impl;

import com.swimmingliu.redislearning.entity.SeckillVoucher;
import com.swimmingliu.redislearning.mapper.SeckillVoucherMapper;
import com.swimmingliu.redislearning.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
