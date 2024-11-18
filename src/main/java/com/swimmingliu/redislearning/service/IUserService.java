package com.swimmingliu.redislearning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.swimmingliu.redislearning.dto.LoginFormDTO;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SwimmingLiu
 * @author  2024-11-15
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result logout();
}
