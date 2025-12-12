package com.orderingsystem.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(SchedulerProperties.class)
public class SchedulingConfig {

    private final SchedulerProperties schedulerProperties;

    @Bean
    public TaskScheduler taskScheduler(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(schedulerProperties.getPoolSize());
        scheduler.setThreadNamePrefix(schedulerProperties.getThreadNamePrefix());

        scheduler.initialize();
        return scheduler;
    }

}
