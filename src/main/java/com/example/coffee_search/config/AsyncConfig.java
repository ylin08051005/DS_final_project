package com.example.coffee_search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "crawlerExecutor")
    public Executor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心執行緒數：設為 20，表示隨時有 20 個工人在待命
        executor.setCorePoolSize(20);
        // 最大執行緒數：設為 50，忙不過來時最多請到 50 人
        executor.setMaxPoolSize(50);
        // 佇列大小：如果 50 人都在忙，還有 500 個工作可以排隊
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Crawler-");
        executor.initialize();
        return executor;
    }
}