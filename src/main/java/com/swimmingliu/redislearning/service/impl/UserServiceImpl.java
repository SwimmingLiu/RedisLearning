package com.swimmingliu.redislearning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swimmingliu.redislearning.context.TokenHolder;
import com.swimmingliu.redislearning.dto.LoginFormDTO;
import com.swimmingliu.redislearning.dto.Result;
import com.swimmingliu.redislearning.dto.UserDTO;
import com.swimmingliu.redislearning.entity.User;
import com.swimmingliu.redislearning.mapper.UserMapper;
import com.swimmingliu.redislearning.service.IUserService;
import com.swimmingliu.redislearning.utils.RegexUtils;
import com.swimmingliu.redislearning.context.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.swimmingliu.redislearning.constant.MessageConstants.*;
import static com.swimmingliu.redislearning.constant.RedisConstants.*;
import static com.swimmingliu.redislearning.constant.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SwimmingLiu
 * @author 2024-11-15
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号是否有效
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail(PHONE_FORMAT_INVALID);
        }
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3. 保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String codeRequest = loginForm.getCode();
        // 1. 判断手机号码是否有效
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail(PHONE_FORMAT_INVALID);
        }
        // 2. 校验手机号和验证码是否对应
        if (RegexUtils.isCodeInvalid(codeRequest)) {
            return Result.fail(LOGIN_CODE_FORMAT_INVALID);
        }
        // 从redis读取phone对应的code
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (code == null) {
            return Result.fail(LOGIN_CODE_EXPIRED);
        } else if (!code.equals(codeRequest)) {
            return Result.fail(LOGIN_CODE_ERROR);
        }
        // 3. 查询手机号用户
        User user = query().eq("phone", phone).one();
        // 4. 判断用户是否存在
        // 4.1 不存在，创建新用户
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        // 4.2 存在，则保存用户信息到redis， 以token为key
        // 生成token
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // beanToMap(需要转成map的数据，map对象（可以有数据），转换配置项（是否忽略空值，字段处理逻辑）)
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> {
                            // 处理null值，避免调用toString引发异常
                            return fieldValue == null ? null : fieldValue.toString();
                        }));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 5. 返回token
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        String token = TokenHolder.getToken();
        // 删除token
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGINOUT_USER_TTL, TimeUnit.SECONDS);
        return Result.ok();

    }

    @Override
    public Result sign() {
        // 1. 获取用户Id
        Long userId = UserHolder.getUser().getId();
        // 2. 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String timeFormat = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));
        String key = USER_SIGN_KEY + userId + timeFormat;
        int dayOfMonth = now.getDayOfMonth();
        // 3. 存入redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1. 获取当前id
        Long userId = UserHolder.getUser().getId();
        // 2. 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String timeFormat = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));
        String key = USER_SIGN_KEY + userId + timeFormat;
        int dayOfMonth = now.getDayOfMonth();
        // 3. 获取Redis当中的签到情况
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0){
            return Result.ok(0);
        }
        // 4. 循环遍历
        int count = 0;
        while(true){
            if ((num & 1) == 0) break;
            else count ++;
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = User.builder()
                .phone(phone)
                .nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10))
                .build();
        save(user);
        return user;
    }
}
