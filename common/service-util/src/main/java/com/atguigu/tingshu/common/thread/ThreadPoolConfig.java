package com.atguigu.tingshu.common.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author: atguigu
 * @create: 2024-08-09 11:28
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 基于JDK（JUC）提供线程池Class
     */
    @Bean
    public Executor threadPoolExecutor() {
        //1.获取当前服务器核心数确定核心线程数
        int cpuCoreCount = Runtime.getRuntime().availableProcessors();
        int threadCount = cpuCoreCount * 2;
        //2.通过构造方法创建线程池对象
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(
                        threadCount,
                        threadCount,
                        0,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(200),
                        Executors.defaultThreadFactory(),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );
        //3.可选：提交创建核心线程
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }


    /**
     * 基于Spring提供线程池Class-threadPoolTaskExecutor 功能更强
     */
    @Bean
    public Executor threadPoolTaskExecutor() {
        int count = Runtime.getRuntime().availableProcessors();
        int threadCount = count*2+1;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 核心池大小
        taskExecutor.setCorePoolSize(threadCount);
        // 最大线程数
        taskExecutor.setMaxPoolSize(threadCount);
        // 队列程度
        taskExecutor.setQueueCapacity(300);
        // 线程空闲时间
        taskExecutor.setKeepAliveSeconds(0);
        // 线程前缀名称
        taskExecutor.setThreadNamePrefix("sync-tingshu-Executor--");
        // 该方法用来设置 线程池关闭 的时候 等待 所有任务都完成后，再继续 销毁 其他的 Bean，
        // 这样这些 异步任务 的 销毁 就会先于 数据库连接池对象 的销毁。
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 任务的等待时间 如果超过这个时间还没有销毁就 强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
        taskExecutor.setAwaitTerminationSeconds(300);
        // 线程不够用时由调用的线程处理该任务
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return taskExecutor;
    }
}