package com.ksoot.spark.common;

import com.ksoot.spark.common.config.SparkConnectorConfiguration;
import com.ksoot.spark.common.config.SpringCloudTaskConfiguration;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.retry.annotation.Retryable;

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ImportAutoConfiguration(
    classes = {SparkConnectorConfiguration.class, SpringCloudTaskConfiguration.class})
public class SpringCommonsConfiguration {

  @ConditionalOnClass(TaskExecution.class)
  static class SparkExecutionManagerConfiguration {

    @Bean
    SparkExecutionManager sparkExecutionManager(
        final SparkSession sparkSession,
        final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry,
        final PushMetricsScheduledTask pushMetricsScheduledTask,
        final MessageSource messageSource) {
      return new SparkExecutionManager(
          sparkSession, kafkaListenerEndpointRegistry, pushMetricsScheduledTask, messageSource);
    }
  }

  @ConditionalOnClass(Retryable.class)
  static class SparkStreamLauncherConfiguration {

    @Bean
    SparkStreamLauncher sparkStreamLauncher(
        final SparkExecutionManager sparkExecutionManager, final TaskExecutor taskExecutor) {
      return new SparkStreamLauncher(sparkExecutionManager, taskExecutor);
    }
  }
}
