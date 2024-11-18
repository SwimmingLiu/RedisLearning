package com.swimmingliu.redislearning.service.impl;

import com.swimmingliu.redislearning.entity.UserInfo;
import com.swimmingliu.redislearning.mapper.UserInfoMapper;
import com.swimmingliu.redislearning.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
