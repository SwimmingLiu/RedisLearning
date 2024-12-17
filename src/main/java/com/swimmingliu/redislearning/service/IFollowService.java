package com.swimmingliu.redislearning.service;

import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long anotherUserId);
}
