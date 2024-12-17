package com.swimmingliu.redislearning.service;

import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);
}
