package com.swimmingliu.redislearning.mapper;

import com.swimmingliu.redislearning.entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author SwimmingLiu
 * @since 2021-12-24
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}
