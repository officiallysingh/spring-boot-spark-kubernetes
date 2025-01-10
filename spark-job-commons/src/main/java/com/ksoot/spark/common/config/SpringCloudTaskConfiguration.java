package com.ksoot.spark.common.config;

import brave.baggage.*;
import brave.context.slf4j.MDCScopeDecorator;
import com.ksoot.spark.common.PushMetricsScheduledTask;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1); // Adjust as needed
    scheduler.setThreadNamePrefix("Scheduler-");
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  PrometheusRegistry prometheusRegistry() {
    PrometheusRegistry prometheusRegistry = PrometheusRegistry.defaultRegistry;
    return prometheusRegistry;
  }

  @Bean
  PushGateway pushGateway(final PrometheusRegistry prometheusRegistry) {
    //    HttpConnectionFactory httpConnectionFactory = new DefaultHttpConnectionFactory();
    PushGateway pushGateway =
        PushGateway.builder()
            .address("localhost:9091") // not needed as localhost:9091 is the default
            .job("sales-report-job")
            .registry(prometheusRegistry)
            .build();
    return pushGateway;
  }

  @Bean
  PushMetricsScheduledTask pushMetricsScheduledTask(
      final ThreadPoolTaskScheduler taskScheduler, final PushGateway pushGateway) {
    return new PushMetricsScheduledTask(taskScheduler, pushGateway);
  }

  @Bean
  CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder() {
    BaggageField TRACE_ID = BaggageFields.constant("traceId", "677fdd923228b9b98c6f7dd19d0e5772");
    BaggageField SPAN_ID = BaggageFields.constant("spanId", "8c6f7dd19d0e5772");
    return MDCScopeDecorator.newBuilder()
        .clear()
        .add(CorrelationScopeConfig.SingleCorrelationField.create(TRACE_ID))
        .add(CorrelationScopeConfig.SingleCorrelationField.create(SPAN_ID));
  }
}
