package com.ksoot.spark;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class SparkJobService {

  private final Environment environment;

  public static void main(final String[] args) {
    SpringApplication.run(SparkJobService.class, args);
  }

  @PostConstruct
  public void init() {
    log.info("Initializing DailySalesReportJob ...");
    if (environment instanceof ConfigurableEnvironment) {
      final List<PropertySource<?>> propertySources =
          ((ConfigurableEnvironment) environment)
              .getPropertySources().stream().collect(Collectors.toList());
      propertySources.forEach(
          propertySource -> {
            System.out.println("Source: " + propertySource.getName());
            if (propertySource.getSource() instanceof java.util.Map) {
              ((java.util.Map<?, ?>) propertySource.getSource())
                  .forEach((key, value) -> System.out.println(key + ": " + value));
            }
          });
    }
  }
}
