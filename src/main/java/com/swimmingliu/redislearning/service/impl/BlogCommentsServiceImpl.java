package com.swimmingliu.redislearning.service.impl;

import com.swimmingliu.redislearning.entity.BlogComments;
import com.swimmingliu.redislearning.mapper.BlogCommentsMapper;
import com.swimmingliu.redislearning.service.IBlogCommentsService;
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
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
