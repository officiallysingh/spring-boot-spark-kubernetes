package com.ksoot.spark.common.config;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.connector.*;
import com.mongodb.spark.sql.connector.MongoCatalog;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.arangodb.datasource.ArangoTable;
import org.apache.spark.sql.kafka010.KafkaRelation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Log4j2
@EnableConfigurationProperties(ConnectorProperties.class)
public class SparkConnectorConfiguration {

  @Bean
  FileConnector fileConnector(
      final SparkSession sparkSession, final ConnectorProperties connectorProperties) {
    return new FileConnector(sparkSession, connectorProperties);
  }

  @Slf4j
  @ConditionalOnClass(org.postgresql.Driver.class)
  static class JdbcConnectorConfiguration {

    @Bean
    JdbcConnector jdbcConnector(
        final SparkSession sparkSession, final ConnectorProperties connectorProperties) {
      return new JdbcConnector(sparkSession, connectorProperties);
    }
  }

  @Slf4j
  @ConditionalOnClass(MongoCatalog.class)
  static class MongoConnectorConfiguration {

    @Bean
    MongoConnector mongoConnector(
        final SparkSession sparkSession, final ConnectorProperties connectorProperties) {
      return new MongoConnector(sparkSession, connectorProperties);
    }
  }

  @Slf4j
  @ConditionalOnClass(ArangoTable.class)
  static class ArangoConnectorConfiguration {

    @Bean
    ArangoConnector arangoConnector(
        final SparkSession sparkSession, final ConnectorProperties connectorProperties) {
      return new ArangoConnector(sparkSession, connectorProperties);
    }
  }

  @ConditionalOnClass(KafkaRelation.class)
  static class KafkaConnectorConfiguration {

    @ConditionalOnMissingBean
    @Bean
    KafkaConnector kafkaConnector(
        final SparkSession sparkSession, final ConnectorProperties connectorProperties) {
      return new KafkaConnector(sparkSession, connectorProperties);
    }
  }
}
