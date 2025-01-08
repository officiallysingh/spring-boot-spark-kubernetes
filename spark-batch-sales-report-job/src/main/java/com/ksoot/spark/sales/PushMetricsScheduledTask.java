package com.ksoot.spark.sales;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class PushMetricsScheduledTask {

  private final PushGateway pushGateway;

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
  public void pushMetrics() throws IOException {
    log.info("Pushing Metrics to push Gateway");
    this.pushGateway.push();
    //        this.pushGateway.delete();
  }
}
