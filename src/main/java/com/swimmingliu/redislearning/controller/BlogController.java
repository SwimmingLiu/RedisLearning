package com.swimmingliu.redislearning.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.entity.Blog;
import com.swimmingliu.redislearning.entity.User;
import com.swimmingliu.redislearning.service.IBlogService;
import com.swimmingliu.redislearning.service.IUserService;
import com.swimmingliu.redislearning.constant.SystemConstants;
import com.swimmingliu.redislearning.context.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.swimmingliu.redislearning.constant.RedisConstants.BLOG_LIKED_KEY;
import static com.swimmingliu.redislearning.constant.SystemConstants.MAX_PAGE_SIZE;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;


    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 用ZSet(sorted_set)实现点赞
     *
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }


    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        });
        return Result.ok(records);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    /**
     * 根据userId查询Blog
     *
     * @param current
     * @param id
     * @return
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "id") Long id) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getUserId, id);
        Page<Blog> blogPage = new Page<>(current, MAX_PAGE_SIZE);
        List<Blog> records = blogService.page(blogPage, wrapper).getRecords();
        return Result.ok(records);
    }

    @GetMapping("/of/follow")
    public Result followerBlogs(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.findFollowerBlogs(max, offset);
    }

    /**
     * 用set实现点赞
     */
//    @PutMapping("/like/{id}")
//    public Result likeBlog(@PathVariable("id") Long id) {
//        // 1. 获取登陆用户
//        Long userId = UserHolder.getUser().getId();
//        // 2. 判断当前用户是否被点赞
//        String key = BLOG_LIKED_KEY + id;
//        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
//        if (BooleanUtil.isTrue(isMember)) {
//            // 当前用户已经点赞 -> 取消点赞
//            // 取消点赞
//            LambdaUpdateWrapper<Blog> wrapper = new LambdaUpdateWrapper<>();
//            wrapper.eq(Blog::getId, id)
//                    .setSql("liked = liked - 1");
//            boolean isSuccess = blogService.update(wrapper);
//            if (isSuccess) {
//                stringRedisTemplate.opsForSet().remove(key, userId.toString());
//            }
//        } else {
//            // 当前用户未点赞 -> 新增点赞
//            // 新增点赞
//            LambdaUpdateWrapper<Blog> wrapper = new LambdaUpdateWrapper<>();
//            wrapper.eq(Blog::getId, id)
//                    .setSql("liked = liked + 1");
//            boolean isSuccess = blogService.update(wrapper);
//            if (isSuccess) {
//                stringRedisTemplate.opsForSet().add(key, userId.toString());
//            }
//        }
//        return Result.ok();
//    }
}
