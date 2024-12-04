package com.swimmingliu.redislearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class RedisLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLearningApplication.class, args);
    }

}
