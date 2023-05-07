package com.yupi.springbootinit.config.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--06 10:19
 * @description 自定义线程池配置类
 */
@Configuration
public class MyThreadConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfig pool){
        return new ThreadPoolExecutor(
                //核心线程数
                pool.getCorePoolSize(),
                //最大线程数
                pool.getMaximumPoolSize(),
                //存活时间
                pool.getKeepAliveTime(),
                //时间单位
                TimeUnit.SECONDS,
                //阻塞队列
                new LinkedBlockingQueue<>(100),
                //线程工程
                Executors.defaultThreadFactory(),
                //拒绝策略
                new ThreadPoolExecutor.AbortPolicy());
    }
}
