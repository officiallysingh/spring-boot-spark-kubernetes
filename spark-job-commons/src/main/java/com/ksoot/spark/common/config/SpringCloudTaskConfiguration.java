package com.ksoot.spark.common.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

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
  JobExecutionListener jobExecutionListener(
      final MessageSource messageSource, final SparkSession sparkSession, final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
    return new JobExecutionListener(messageSource, sparkSession, kafkaListenerEndpointRegistry);
  }

  @Bean
  public TaskExecutor taskExecutor() {
    return new SyncTaskExecutor();
  }
}
