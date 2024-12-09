package com.ksoot.spark.conf;

import com.ksoot.spark.util.pagination.PaginatedResourceAssembler;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;

@Configuration
@EnableConfigurationProperties(SparkSubmitProperties.class)
@RequiredArgsConstructor
class SparkJobServiceConfiguration {

  private static final String SPARK_PREFIX = "spark.";

  //  private final SparkSubmitProperties sparkSubmitProperties;

  @Bean
  Properties sparkProperties(final Environment environment) {
    if (environment instanceof ConfigurableEnvironment) {
      final List<PropertySource<?>> propertySources =
          ((ConfigurableEnvironment) environment)
              .getPropertySources().stream().collect(Collectors.toList());
      final List<String> sparkPropertyNames =
          propertySources.stream()
              .filter(propertySource -> propertySource instanceof EnumerablePropertySource)
              .map(propertySource -> (EnumerablePropertySource) propertySource)
              .map(EnumerablePropertySource::getPropertyNames)
              .flatMap(Arrays::stream)
              .distinct()
              .filter(key -> key.startsWith(SPARK_PREFIX))
              .collect(Collectors.toList());

      return sparkPropertyNames.stream()
          .collect(
              Properties::new,
              (props, key) -> props.put(key, environment.getProperty(key)),
              Properties::putAll);
    } else {
      return new Properties();
    }
  }

  @Bean
  PaginatedResourceAssembler paginatedResourceAssembler(
      @Nullable final HateoasPageableHandlerMethodArgumentResolver resolver) {
    return new PaginatedResourceAssembler(resolver);
  }
}
