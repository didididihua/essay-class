package com.yupi.springbootinit.config.thread;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--06 10:20
 * @description 用于读取配置的数据给自定义线程池进行初始化
 */
@Data
@ConfigurationProperties(prefix = "post.thread")
@Component
public class ThreadPoolConfig {
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Integer keepAliveTime;
}
