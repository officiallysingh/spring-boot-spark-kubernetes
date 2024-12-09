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
import org.springframework.util.Assert;

@Slf4j
@RequiredArgsConstructor
public class MongoConnector {

  protected final SparkSession sparkSession;

  protected final ConnectorProperties properties;

  public Dataset<Row> read(final String collection) {
    log.info(
        "Reading from MongoDB >> database: {}, collection: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection);
    Assert.hasText(collection, "MongoDB collection name required");
    return this.sparkSession
        .read()
        .format(SparkOptions.Mongo.FORMAT)
        .options(this.properties.getMongoOptions().options(collection))
        .option(SparkOptions.Common.INFER_SCHEMA, true)
        .load();
  }

  public Dataset<Row> read(final String collection, final StructType schema) {
    log.info(
        "Reading from MongoDB >> database: {}, collection: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection);
    Assert.hasText(collection, "MongoDB collection name required");
    return this.sparkSession
        .read()
        .format(SparkOptions.Mongo.FORMAT)
        .schema(schema)
        .options(this.properties.getMongoOptions().options(collection))
        .load();
  }

  public Dataset<Row> read(final String collection, final String aggregationPipeline) {
    Assert.hasText(collection, "MongoDB collection name required");
    Assert.hasText(aggregationPipeline, "MongoDB aggregationPipeline required");
    log.info(
        "Reading from MongoDB >> database: {}, collection: {}, aggregationPipeline: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection,
        aggregationPipeline);
    return this.sparkSession
        .read()
        .format(SparkOptions.Mongo.FORMAT)
        .options(this.properties.getMongoOptions().options(collection))
        .option(SparkOptions.Common.INFER_SCHEMA, true)
        .option(SparkOptions.Mongo.AGGREGATION_PIPELINE, aggregationPipeline)
        .load();
  }

  public Dataset<Row> read(
      final String collection, final String aggregationPipeline, final StructType schema) {
    Assert.hasText(collection, "MongoDB collection name required");
    Assert.hasText(aggregationPipeline, "MongoDB aggregationPipeline required");
    log.info(
        "Reading from MongoDB >> database: {}, collection: {}, aggregationPipeline: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection,
        aggregationPipeline);
    return this.sparkSession
        .read()
        .format(SparkOptions.Mongo.FORMAT)
        .schema(schema)
        .options(this.properties.getMongoOptions().options(collection))
        .option(SparkOptions.Mongo.AGGREGATION_PIPELINE, aggregationPipeline)
        .load();
  }

  public void write(final Dataset<Row> dataset, final String collection) {
    Assert.hasText(collection, "MongoDB collection name required");
    log.info(
        "Writing to MongoDB >> database: {}, collection: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection);
    dataset
        .write()
        .format(SparkOptions.Mongo.FORMAT)
        .mode(this.properties.getSaveMode())
        .options(this.properties.getMongoOptions().options(collection))
        .save();
  }

  public DataStreamWriter<Row> writeStream(final Dataset<Row> dataset, final String collection) {
    Assert.hasText(collection, "MongoDB collection name required");
    log.info(
        "Streaming to MongoDB >> database: {}, collection: {}",
        this.properties.getMongoOptions().getDatabase(),
        collection);
    return dataset
        .writeStream()
        .format(SparkOptions.Mongo.FORMAT)
        .outputMode(this.properties.outputMode())
        .options(this.properties.getMongoOptions().options(collection))
        .option(SparkOptions.Common.CHECKPOINT_LOCATION, this.properties.getCheckpointLocation());
  }
}
