package com.swimmingliu.redislearning.mapper;

import com.swimmingliu.redislearning.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author SwimmingLiu
 * @since 2022-01-04
 */
@Mapper
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
