package com.ksoot.spark.common;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

@Log4j2
@RequiredArgsConstructor
public class PushMetricsScheduledTask {

  private final ThreadPoolTaskScheduler taskScheduler;

  private final PushGateway pushGateway;

  private ScheduledFuture<?> scheduledTask;

  @PostConstruct
  public void startTask() {
    log.info("Starting PushMetrics Scheduled Task...");
    this.scheduledTask =
        this.taskScheduler.schedule(this::pushMetrics, new PeriodicTrigger(Duration.ofSeconds(3)));
  }

  private void pushMetrics() {
    try {
      log.info("Pushing Metrics to Push Gateway");
      this.pushGateway.push();
    } catch (IOException e) {
      log.error("Error while pushing metrics", e);
    }
  }

  @PreDestroy
  public void stopTask() {
    if (this.scheduledTask != null) {
      log.info("Stopping PushMetrics Scheduled Task...");
      this.scheduledTask.cancel(true);
    }
    this.taskScheduler.shutdown();
  }
}
