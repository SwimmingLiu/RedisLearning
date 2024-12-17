package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swimmingliu.redislearning.context.UserHolder;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.entity.Follow;
import com.swimmingliu.redislearning.entity.User;
import com.swimmingliu.redislearning.mapper.FollowMapper;
import com.swimmingliu.redislearning.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swimmingliu.redislearning.constant.RedisConstants.FOLLOW_USER_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    /**
     * 关注该用户
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 获取当前用户ID
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_USER_KEY + userId;
        // 2. 关注或取消关注
        if (isFollow) { //关注
            Follow follow = Follow.builder()
                    .userId(userId)
                    .followUserId(followUserId)
                    .createTime(LocalDateTime.now())
                    .build();
            boolean isSuccess = this.save(follow);
            if (isSuccess){ // 同步新增Redis数据
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else { // 取消关注
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowUserId, followUserId)
                    .eq(Follow::getUserId, userId);
            boolean isSuccess = remove(wrapper);
            if (isSuccess){ // 同步删除Redis数据
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 判断当前用户是否关注followUser
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        return Result.ok(count(wrapper) > 0);
    }

    /**
     * 查询共同关注
     * @param anotherUserId
     * @return
     */
    @Override
    public Result followCommons(Long anotherUserId) {
        // 1. 获取当前的UserID
        Long userId = UserHolder.getUser().getId();
        // 2. 获取当前用户关注人物和另外一个用户关注人物的交集
        String userKey = FOLLOW_USER_KEY + userId;
        String anotherUserKey = FOLLOW_USER_KEY + anotherUserId;
        Set<String> mutualFollowerIds = stringRedisTemplate.opsForSet().intersect(userKey, anotherUserKey);
        // 3. 判断共同关注的列表是否为空
        if (mutualFollowerIds == null || mutualFollowerIds.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        // 4. 获取共同关注用户的信息
        List<Long> ids = mutualFollowerIds.stream().map(Long::valueOf).toList();
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
