package com.ksoot.spark.common.connector;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.util.SparkOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.types.StructType;

@Slf4j
@RequiredArgsConstructor
public class KafkaConnector {

  protected final SparkSession sparkSession;

  protected final ConnectorProperties properties;

  public Dataset<Row> readStream(final String topic) {
    Dataset<Row> dataset =
        this.sparkSession
            .readStream()
            .format(SparkOptions.Kafka.FORMAT)
            .option(SparkOptions.Kafka.SUBSCRIBE, topic)
            .options(this.properties.getKafkaOptions().readOptions())
            .load()
        //  .selectExpr("CAST(value AS STRING) as json_string")
        ;
    return dataset;
  }

  public Dataset<Row> readStream(final String topic, final StructType schema) {
    Dataset<Row> dataset =
        this.sparkSession
            .readStream()
            .schema(schema)
            .format(SparkOptions.Kafka.FORMAT)
            .option(SparkOptions.Kafka.SUBSCRIBE, topic)
            .options(this.properties.getKafkaOptions().readOptions())
            .load()
        //  .selectExpr("CAST(value AS STRING) as json_string")
        ;
    return dataset;
  }

  public void write(final Dataset<Row> dataset) {
    log.info(
        "Streaming data to Kafka: {} topic: {}",
        this.properties.getKafkaOptions().getBootstrapServers(),
        this.properties.getKafkaOptions().getTopic());
    dataset
        // // The lambda used in following map method must be Serializable
        // .map(
        // // Mapper function in case you want to convert dataset to another object and write that
        // object to kafka topic. It must be serializable
        //
        //  ,
        // // Some Encoder as per your need, for eg.
        // Encoders.BINARY()
        // )
        .write()
        .format(SparkOptions.Kafka.FORMAT)
        .options(this.properties.getKafkaOptions().writeOptions())
        .save();
  }

  public DataStreamWriter<Row> writeStream(final Dataset<Row> dataset) {
    return dataset
        // // The lambda used in following map method must be Serializable
        // .map(
        // // Mapper function in case you want to convert dataset to another object and write that
        // object to kafka topic. It must be serializable
        //
        //  ,
        // // Some Encoder as per your need, for eg.
        // Encoders.BINARY()
        // )
        .writeStream()
        .format(SparkOptions.Kafka.FORMAT)
        .options(this.properties.getKafkaOptions().writeOptions());
  }
}
