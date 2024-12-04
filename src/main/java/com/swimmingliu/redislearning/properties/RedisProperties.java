package com.swimmingliu.redislearning.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dianping.redis")
public class RedisProperties {
    public String host;
    public String port;
    public String password;
    public Integer databaseNum;
}
