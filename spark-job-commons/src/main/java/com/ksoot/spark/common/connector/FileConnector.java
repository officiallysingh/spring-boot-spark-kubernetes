package com.ksoot.spark.common.connector;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.util.SparkOptions;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.types.StructType;

@Slf4j
@RequiredArgsConstructor
public class FileConnector {

  protected final SparkSession sparkSession;

  protected final ConnectorProperties properties;

  public Dataset<Row> read(final String format, final String file, final StructType schema) {
    return this.sparkSession
        .read()
        .format(format)
        .schema(schema)
        .options(this.properties.getFileOptions().readOptions())
        .load(file);
  }

  public Dataset<Row> read(final String format, final String file) {
    return this.sparkSession
        .read()
        .format(format)
        .option(SparkOptions.Common.INFER_SCHEMA, true)
        .options(this.properties.getFileOptions().readOptions())
        .load(file);
  }

  public void write(final Dataset<Row> dataset) {
    Dataset<Row> result = dataset;
    result =
        this.properties.getFileOptions().isMerge()
            ? result.coalesce(1) // Write to a single file
            : result;
    log.info(
        "Writing output at location: {} in {} format.",
        this.properties.getFileOptions().getPath(),
        this.properties.getFileOptions().getFormat());
    result
        .write()
        .mode(this.properties.getSaveMode())
        .format(this.properties.getFileOptions().getFormat())
        .options(this.properties.getFileOptions().writeOptions())
        .save();
  }

  public void write(final Dataset<Row> dataset, final Map<String, String> options) {
    Dataset<Row> result = dataset;
    result =
        this.properties.getFileOptions().isMerge()
            ? result.coalesce(1) // Write to a single file
            : result;
    log.info(
        "Writing output at location: {} in {} format.",
        this.properties.getFileOptions().getPath(),
        this.properties.getFileOptions().getFormat());
    result
        .write()
        .mode(this.properties.getSaveMode())
        .format(this.properties.getFileOptions().getFormat())
        .options(this.properties.getFileOptions().writeOptions())
        .options(options)
        .save();
  }

  public DataStreamWriter<Row> writeStream(
      final Dataset<Row> dataset, final Map<String, String> options) {
    Dataset<Row> result = dataset;
    result =
        this.properties.getFileOptions().isMerge()
            ? result.coalesce(1) // Write to a single file
            : result;
    log.info(
        "Streaming output at location: {} in {} format.",
        this.properties.getFileOptions().getPath(),
        this.properties.getFileOptions().getFormat());
    return result
        .writeStream()
        .outputMode(this.properties.outputMode())
        .format(this.properties.getFileOptions().getFormat())
        .options(this.properties.getFileOptions().writeOptions())
        .options(options)
        .option(SparkOptions.Common.CHECKPOINT_LOCATION, this.properties.getCheckpointLocation());
  }
}
