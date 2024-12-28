package com.swimmingliu.redislearning.controller;


import cn.hutool.core.bean.BeanUtil;
import com.swimmingliu.redislearning.dto.LoginFormDTO;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.entity.User;
import com.swimmingliu.redislearning.entity.UserInfo;
import com.swimmingliu.redislearning.service.IUserInfoService;
import com.swimmingliu.redislearning.service.IUserService;
import com.swimmingliu.redislearning.context.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.swimmingliu.redislearning.constant.MessageConstants.USER_NOT_FOUND;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户登录")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;


    /**
     * 发送手机验证码
     */
    @Operation(summary = "发送手机验证码")
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Operation(summary = "登录功能")
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @Operation(summary = "注销功能")
    @PostMapping("/logout")
    public Result logout(){
        return userService.logout();
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @Operation(summary = "根据id获取个人信息")
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 根据ID查询用户信息
     * @param id
     * @return
     */
     @Operation(summary = "根据ID查询用户信息")
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long id){
        User user = userService.getById(id);
        if (user == null){
            return Result.fail(USER_NOT_FOUND);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    /**
     * 用户签到
     * @return
     */
    @Operation(summary = "用户签到")
    @PutMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 用户签到统计
     * @return
     */
    @Operation(summary = "用户签到统计")
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
