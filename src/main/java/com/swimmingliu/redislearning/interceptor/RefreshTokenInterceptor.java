package com.swimmingliu.redislearning.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.swimmingliu.redislearning.context.TokenHolder;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.context.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.RedisConstants.LOGIN_USER_KEY;
import static com.swimmingliu.redislearning.constant.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {


    private StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.判断用户提供的token是否存在
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            // 直接放行
            return true;
        }
        // 2.判断token是否过期
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if (userMap.isEmpty()){
            return true;
        }

        // 3. ThreadLocal保存User信息
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);
        UserHolder.saveUser(userDTO);

        // 4.刷新token时间
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 5.保存Token
        TokenHolder.setToken(token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清空ThreadLocal
        UserHolder.removeUser();
        TokenHolder.removeToken();
    }
}
