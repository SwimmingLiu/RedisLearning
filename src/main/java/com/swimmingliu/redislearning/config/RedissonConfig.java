package com.swimmingliu.redislearning.config;

import com.swimmingliu.redislearning.properties.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Autowired
    private RedisProperties redisProperties;
    @Bean
    public RedissonClient redissonClient() {
        // 设置配置信息
        Config redissonConfig = new Config();
        String redisAddress = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        redissonConfig.useSingleServer().setAddress(redisAddress)
                .setPassword(redisProperties.getPassword())
                .setDatabase(redisProperties.getDatabaseNum());
        // 创建RedissonClient对象
        return Redisson.create(redissonConfig);
    }
}
