package com.ksoot.spark;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Log4j2
@RequiredArgsConstructor
public class SparkJobService {

  private final Environment environment;

  public static void main(final String[] args) {
    SpringApplication.run(SparkJobService.class, args);
  }

  @PostConstruct
  public void init() {
    log.info("Logging Spring boot environment ...");
    // Just for debugging, otherwise sensitive info could be logged.
    //    if (environment instanceof ConfigurableEnvironment) {
    //      final List<PropertySource<?>> propertySources =
    //          ((ConfigurableEnvironment) environment)
    //              .getPropertySources().stream().collect(Collectors.toList());
    //      propertySources.forEach(
    //          propertySource -> {
    //            System.out.println("Source: " + propertySource.getName());
    //            if (propertySource.getSource() instanceof java.util.Map) {
    //              ((java.util.Map<?, ?>) propertySource.getSource())
    //                  .forEach((key, value) -> System.out.println(key + ": " + value));
    //            }
    //          });
    //    }
  }
}
