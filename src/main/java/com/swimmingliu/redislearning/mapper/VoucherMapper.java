package com.swimmingliu.redislearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swimmingliu.redislearning.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
