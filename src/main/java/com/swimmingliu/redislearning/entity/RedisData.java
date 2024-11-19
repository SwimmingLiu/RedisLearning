package com.swimmingliu.redislearning.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedisData {
    private LocalDateTime ttl; // 过期时间
    private Object data; // 存放数据
}
