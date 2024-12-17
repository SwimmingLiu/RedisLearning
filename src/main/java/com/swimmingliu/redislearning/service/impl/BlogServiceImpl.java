package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.swimmingliu.redislearning.context.UserHolder;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.entity.Blog;
import com.swimmingliu.redislearning.entity.User;
import com.swimmingliu.redislearning.mapper.BlogMapper;
import com.swimmingliu.redislearning.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swimmingliu.redislearning.constant.MessageConstants.BLOG_NOT_FOUND;
import static com.swimmingliu.redislearning.constant.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    /**
     * 根据ID查询Blog
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogById(Long id) {
        // 1. 查询当前blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail(BLOG_NOT_FOUND);
        }
        // 2. 查询blog相关的user
        queryRelatedUser(blog);
        // 3. 查询blog是否被点赞
        queryBlogIsLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 查询博客是否被点赞
     *
     * @param blog
     */
    private void queryBlogIsLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        if (userId == null) {
            // 用户未登录，无需判断是否点赞博客
            return;
        }
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 查询Blog相关的User信息
     *
     * @param blog
     */
    private void queryRelatedUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 点赞Blog
     *
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        // 1. 获取登陆用户
        Long userId = UserHolder.getUser().getId();
        // 2. 判断当前用户是否被点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score != null) {  // 当前用户已经点赞 -> 取消点赞
            // 取消点赞
            LambdaUpdateWrapper<Blog> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Blog::getId, id)
                    .setSql("liked = liked - 1");
            boolean isSuccess = this.update(wrapper);
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        } else { // 当前用户未点赞 -> 新增点赞
            // 新增点赞
            LambdaUpdateWrapper<Blog> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Blog::getId, id)
                    .setSql("liked = liked + 1");
            boolean isSuccess = this.update(wrapper);
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        return Result.ok();
    }

    /**
     * 查询Blog点赞的用户
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        // 1. 查询这篇博客是否存在
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail(BLOG_NOT_FOUND);
        }
        // 2. 判断有那些用户点赞了这篇博客
        String key = BLOG_LIKED_KEY + id;
        // 查询TOP5的点赞用户
        Set<String> userIds = stringRedisTemplate.opsForZSet().range(key, 0 , 4);
        if (userIds == null || userIds.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        String userIdsStr = StrUtil.join(",", userIds);
        // 数据库按照ZSet排序的结果返回
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, userIds)
                .last("order by FIELD(id," + userIdsStr + ")");
        List<UserDTO> userDTOS = userService.list(wrapper)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

}
