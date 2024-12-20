package com.ksoot.spark.loganalysis.conf;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
@EnableConfigurationProperties
class JobConfiguration {

  @Bean
  public NewTopic errorLogsTopic(final ConnectorProperties connectorProperties) {
    return TopicBuilder.name(connectorProperties.getKafkaOptions().getTopic()).build();
  }
}
