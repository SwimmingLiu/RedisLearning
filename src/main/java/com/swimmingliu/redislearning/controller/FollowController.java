package com.swimmingliu.redislearning.controller;


import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
                         @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 判断用户是否已关注
     * @param followUserId
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long anotherUserId){
        return followService.followCommons(anotherUserId);
    }


}
