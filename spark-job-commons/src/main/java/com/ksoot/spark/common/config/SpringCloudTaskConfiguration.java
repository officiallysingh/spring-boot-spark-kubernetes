package com.ksoot.spark.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(TaskExecution.class)
public class SpringCloudTaskConfiguration {

  @ConditionalOnProperty(prefix = "ksoot.job", name = "persist", havingValue = "false")
  @Bean
  @Primary
  // To make Spring cloud task to not use any database but in memory only.
  DefaultTaskConfigurer taskConfigurer() {
    return new DefaultTaskConfigurer(TaskProperties.DEFAULT_TABLE_PREFIX);
  }

  @Bean
  public TaskExecutor taskExecutor() {
    // Async Task executor must not be used, Spark need to work in synchronously
    return new SyncTaskExecutor();
  }
}
