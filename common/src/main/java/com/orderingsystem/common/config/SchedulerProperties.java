package com.orderingsystem.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {

    private int poolSize = 5;
    private String threadNamePrefix = "CommonScheduler-";

}
