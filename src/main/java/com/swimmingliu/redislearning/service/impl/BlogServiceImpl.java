package com.swimmingliu.redislearning.service.impl;

import com.swimmingliu.redislearning.entity.Blog;
import com.swimmingliu.redislearning.mapper.BlogMapper;
import com.swimmingliu.redislearning.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
