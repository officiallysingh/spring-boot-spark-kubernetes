package com.ksoot.spark.loganalysis;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.regexp_extract;

import com.ksoot.spark.common.SparkExecutionManager;
import com.ksoot.spark.common.SparkStreamLauncher;
import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.connector.JdbcConnector;
import com.ksoot.spark.common.connector.KafkaConnector;
import com.ksoot.spark.common.util.StreamRetryableException;
import com.ksoot.spark.loganalysis.conf.JobProperties;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SparkPipelineExecutor {

  private static final String LOG_REGEX =
      "(?<=^)(\\S+T\\S+)(\\s+ERROR\\s+\\d+\\s+---\\s+\\[([a-zA-Z0-9-]+)\\].*)(:.*)";

  private static final String ERROR_LOGS_TABLE = "error_logs";

  private final SparkSession sparkSession;

  private final JobProperties jobProperties;

  private final ConnectorProperties connectorProperties;

  private final KafkaConnector kafkaConnector;

  private final JdbcConnector jdbcConnector;

  private final SparkStreamLauncher sparkStreamLauncher;

  public void execute() {
    Dataset<Row> kafkaLogs =
        this.kafkaConnector.readStream(this.connectorProperties.getKafkaOptions().getTopic());
    // Deserialize Kafka messages as text
    Dataset<Row> logLines = kafkaLogs.selectExpr("CAST(value AS STRING) as log_line");
    // Just for testing
    //    this.writeToConsole(errorLogs);

    Dataset<Row> errorLogs =
        logLines
            .filter(col("log_line").rlike(LOG_REGEX))
            .select(
                regexp_extract(col("log_line"), LOG_REGEX, 1).alias("datetime"),
                regexp_extract(col("log_line"), LOG_REGEX, 3).alias("application"),
                regexp_extract(col("log_line"), LOG_REGEX, 4).alias("error_message"));

    DataStreamWriter<Row> logsStreamWriter =
        this.jdbcConnector.writeStream(errorLogs, ERROR_LOGS_TABLE);
    // Start the stream in separate thread
    this.sparkStreamLauncher.startStream(logsStreamWriter);
  }

  // Just for testing
  private void writeToConsole(final Dataset<Row> errorLogs) {
    try {
      errorLogs
          .writeStream()
          .outputMode(this.connectorProperties.getOutputMode())
          .format("console")
          .start()
          .awaitTermination();
    } catch (final TimeoutException | StreamingQueryException e) {
      log.error("Error while writing to Console", e);
    }
  }
}
