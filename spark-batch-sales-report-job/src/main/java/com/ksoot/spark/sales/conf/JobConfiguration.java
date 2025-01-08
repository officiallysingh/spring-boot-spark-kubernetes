package com.ksoot.spark.sales.conf;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(JobProperties.class)
class JobConfiguration {

  //    @Bean
  //    public PushGatewayMeterRegistry pushGatewayMeterRegistry() {
  //        PushGateway pushGateway = new PushGateway(pushGatewayUrl);
  //        PushGatewayMeterRegistry registry = new PushGatewayMeterRegistry((config) -> {
  //            config.commonTags("application", applicationName);
  //            config.step(Duration.ofSeconds(10)); // Push interval
  //        }, pushGateway, new SimpleMeterRegistry());
  //        registry.start(); // Starts periodic pushing
  //        return registry;
  //    }

  //  @Bean
  //  public SimpleMeterRegistry meterRegistry() {
  //    return new SimpleMeterRegistry();
  //  }

  @Bean
  PrometheusRegistry prometheusRegistry() {
    PrometheusRegistry prometheusRegistry = PrometheusRegistry.defaultRegistry;
    return prometheusRegistry;
  }

  @Bean
  public PushGateway pushGateway(final PrometheusRegistry prometheusRegistry) {

    PushGateway pushGateway =
        PushGateway.builder()
            .address("localhost:9091") // not needed as localhost:9091 is the default
            .job("sales-report-job")
            .registry(prometheusRegistry)
            .build();
    return pushGateway;
  }
}
