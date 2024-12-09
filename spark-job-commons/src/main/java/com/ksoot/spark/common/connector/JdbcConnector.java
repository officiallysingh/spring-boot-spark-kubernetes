package com.ksoot.spark.common.connector;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.util.SparkOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.types.StructType;

@Slf4j
@RequiredArgsConstructor
public class JdbcConnector {

  protected final SparkSession sparkSession;

  protected final ConnectorProperties properties;

  public Dataset<Row> read(final String table) {
    Dataset<Row> dataFrame =
        this.sparkSession
            .read()
            .options(this.properties.getJdbcOptions().readOptions())
            .jdbc(
                this.properties.getJdbcOptions().getUrl(),
                table,
                this.properties.getJdbcOptions().connectionProperties());
    return dataFrame;
  }

  public Dataset<Row> read(final String table, final StructType schema) {
    Dataset<Row> dataFrame =
        this.sparkSession
            .read()
            .schema(schema)
            .options(this.properties.getJdbcOptions().readOptions())
            .jdbc(
                this.properties.getJdbcOptions().getUrl(),
                table,
                this.properties.getJdbcOptions().connectionProperties());
    return dataFrame;
  }

  public void write(final Dataset<Row> dataset, final String table) {
    dataset
        .write()
        .mode(this.properties.getSaveMode())
        .options(this.properties.getJdbcOptions().writeOptions())
        .jdbc(
            this.properties.getJdbcOptions().getUrl(),
            table,
            this.properties.getJdbcOptions().connectionProperties());
  }

  public DataStreamWriter<Row> writeStream(final Dataset<Row> dataset, final String table) {
    log.info(
        "Streaming data to database: {} table: {}",
        this.properties.getJdbcOptions().getDatabase(),
        table);
    // Write each micro-batch to PostgreSQL
    return dataset
        .writeStream()
        .outputMode(this.properties.outputMode())
        .options(this.properties.getJdbcOptions().writeOptions())
        .foreachBatch(
            (batchDataset, batchId) -> {
              batchDataset
                  .write()
                  .mode(this.properties.getSaveMode())
                  .jdbc(
                      this.properties.getJdbcOptions().getUrl(),
                      table,
                      this.properties.getJdbcOptions().connectionProperties());
            })
        .option(SparkOptions.Common.CHECKPOINT_LOCATION, this.properties.getCheckpointLocation());
  }
}
