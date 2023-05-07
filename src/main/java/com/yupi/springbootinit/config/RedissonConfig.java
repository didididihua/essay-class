package com.yupi.springbootinit.config;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 21:10
 * @description redisson的配置类
 */

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

     @Bean
     public RedissonClient redissonClient() {
         //1、创建配置
         String url = host + ":" + port;
         Config config = new Config();
         config.useSingleServer().setAddress("redis://43.143.192.202:6388")
                 .setPassword("123456")
                 .setPingConnectionInterval(1000)
                 .setRetryAttempts(3)
                 .setRetryInterval(1000);

         //2、根据Config创建出RedissonClient实例
         //Redis url should start with redis:// or rediss://
         RedissonClient redissonClient = Redisson.create(config);
         return redissonClient;
     }

}
