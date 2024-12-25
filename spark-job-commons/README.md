# Spark Job Commons
Provides the common components and utilities to be used across Spark Jobs to reduce boilerplate and enhance reusability.
Spark jobs includes it as a dependency.
```xml
<dependency>
    <groupId>com.ksoot.spark</groupId>
    <artifactId>spark-job-commons</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Connectors
Spark can connect with almost any datasource, followings are configurable connectors readily available.
More connectors can be added similarly, existing can also be modified as per requirements. Refer to [ConnectorProperties.java](src/main/java/com/ksoot/spark/common/config/properties/ConnectorProperties.java) for connector configurations.
```yaml
ksoot:
  connector:
    save-mode: Append
    output-mode: Update
```
**Description**
* `ksoot.connector.save-mode`:- To specify the expected behavior of saving a DataFrame to a data source, with Default value as `Append`. Applicable in Spark **Batch** jobs only. Allowed values: `Append`, `Overwrite`, `ErrorIfExists`, `Ignore`.
* `ksoot.connector.output-mode`:- Describes what data will be written to a streaming sink when there is new data available in a streaming Dataset, with Default value as `Append`. Applicable in Spark **Streaming** jobs only. Allowed values: `Append`, `Complete`, `Update`.

#### File Connector
To read and write to files of various formats in batch and streaming mode. Refer to [FileConnector.java](src/main/java/com/ksoot/spark/common/connector/FileConnector.java) for details.
It can be customized with the following configurations as follows. Refer to [FileOptions.java](src/main/java/com/ksoot/spark/common/config/properties/FileOptions.java) for details on available configuration options.
```yaml
ksoot:
  connector:
    file-options:
      format: csv
      header: true
      path: spark-space/output
      merge: true
```

#### MongoDB Connector
To read and write to MongoDB collections in batch and streaming mode. Refer to [MongoConnector.java](src/main/java/com/ksoot/spark/common/connector/MongoConnector.java) for details.
Following database connection configurations are required as follows. Refer to [MongoOptions.java](src/main/java/com/ksoot/spark/common/config/properties/MongoOptions.java) for details on available configuration options.
```yaml
ksoot:
  connector:
    mongo-options:
      url: mongodb://localhost:27017
      database: sales_db
```

#### ArangoDB Connector
To read and write to ArangoDB collections in batch and streaming mode. Refer to [ArangoConnector.java](src/main/java/com/ksoot/spark/common/connector/ArangoConnector.java) for details.
Expects to be provided with basic database connection configurations as follows. Refer to [ArangoOptions.java](src/main/java/com/ksoot/spark/common/config/properties/ArangoOptions.java) for details on available configuration options.
```yaml
ksoot:
  connector:
    arango-options:
      endpoints: localhost:8529
      database: products_db
      username: root
      password: admin
      ssl-enabled: false
      ssl-cert-value: ""
      cursor-ttl: PT5M # 5 minutes, see the ISO 8601 standard for java.time.Duration String patterns
```

#### JDBC Connector
To read and write to JDBC database tables in batch and streaming mode. Refer to [JdbcConnector.java](src/main/java/com/ksoot/spark/common/connector/JdbcConnector.java) for details.
Expects to be provided with basic database connection configurations along with few customization parameters as follows. Refer to [JdbcOptions.java](src/main/java/com/ksoot/spark/common/config/properties/JdbcOptions.java) for details on available configuration options.
```yaml
ksoot:
  connector:
    jdbc-options:
      url: jdbc:postgresql://localhost:5432
      database: error_logs_db
      username: postgres
      password: admin
      fetchsize: 100
      batchsize: 1000
      isolation-level: READ_UNCOMMITTED
```

#### Kafka Connector
To read and write to JDBC database tables in batch and streaming mode. Refer to [KafkaConnector.java](src/main/java/com/ksoot/spark/common/connector/KafkaConnector.java) for details.
Expects to be provided with basic kafka connection configurations along with few customization parameters as follows. Refer to [KafkaOptions.java](src/main/java/com/ksoot/spark/common/config/properties/KafkaOptions.java) for details on available configuration options.
```yaml
ksoot:
  connector:
    kafka-options:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    topic: ${KAFKA_ERROR_LOGS_TOPIC:error-logs}
    fail-on-data-loss: ${KAFKA_FAIL_ON_DATA_LOSS:false}
```

## Job Listener
An auto-configured Job listener with the following features. Refer to [SparkExecutionManager.java](src/main/java/com/ksoot/spark/common/SparkExecutionManager.java) for details.
- Log Job startup and completed on timestamps, time taken by Job.
- Sets status as `Completed` for successful completion, `Failed` for failed Jobs and `Terminated` if stopped prematurely as per Job stop request.
- Set an exit message as Job status.
- Log errors as per thrown exception in case of Job exit with failures.
- Listens to kafka topic `job-stop-requests` to terminate a running job.
> [!IMPORTANT]
> On Job stop request arrival, it just tries to stop `SparkContext`, but it is not guaranteed method to stop the running job.
> Sometimes the kafka listener may face thread starvation issue, as the available threads could be overwhelmed by Spark job and listener may never get a chance to execute.
> Or even after calling stop method on `SparkContext`, the job may not stop immediately. 
> To force-stop the job you may need to find a mechanism to kill the job. Kill from **Spark UI** or [**`spark-submit.sh --kill 20160615124014699000`**](https://www.ibm.com/docs/en/ias?topic=spark-canceling-running-application#d141740e99).

## Spark Stream Launcher
An auto-configured Launch Spark streams with following features. Refer to [SparkStreamLauncher.java](src/main/java/com/ksoot/spark/common/SparkStreamLauncher.java) for details.
- Start and await on Spark stream in a separate thread, so that multiple streams can be started without blocking the main thread.
- Optional, Retry mechanism to restart the stream in case of failure.

## Exceptions
It is discouraged to create a lot of custom exceptions. Only one custom exception class [JobProblem.java](src/main/java/com/ksoot/spark/common/error/JobProblem.java) can be used to throw exceptions as follows.

```java
try {
    // Some file reading code
} catch (final IOException e) {
  throw JobProblem.of("IOException while listing file by reading from aws").cause(e).build();
}
```

[JobProblem.java](src/main/java/com/ksoot/spark/common/error/JobProblem.java) provides a fluent builder pattern to build its instance.
* Create an exception with hardcoded message. Optionally can set a cause exception and runtime arguments to inject into message placeholders.
```java
JobProblem.of("Error while reading file: {}").cause(fileNotFoundException).args("/home/data/input.csv").build();
```
* Recommended to create Enums as follows to define error codes and messages.
```java
public enum PipelineErrors implements ErrorType {
  SPARK_STREAMING_EXCEPTION(
      "spark.streaming.exception", "Pipeline Exception", "Spark Streaming exception"),
  INVALID_CONFIGURATION(
      "invalid.configuration",
      "Configuration Error",
      "Invalid conguration value: {}, allowed values are: {}");

  // Skipping constructor and other methods
}
```
This way the error title and message can be customized in `messages.properties` or any other configured resource bundle as follows.  
Note in following message keys, `title` and `message` prefix to error codes mentioned in above enum.
```text
title.spark.streaming.exception=My custom title
message.spark.streaming.exception=My custom message, check param: {0}, {1}ception=My custom title
title.invalid.configuration=Some title
message.invalid.configuration=Some message
```

## Utilites
#### [SparkUtils](src/main/java/com/ksoot/spark/common/util/SparkUtils.java) 
* Convert column names to Spark Column objects.
* Check if a dataset contains specific columns.
* Wrap column names with backticks if they contain slashes.
* Log the schema and content of a dataset, see method `public static void logDataset(final String datasetName, final Dataset<Row> dataset, final int numRows)`.

#### [SparkOptions](src/main/java/com/ksoot/spark/common/util/SparkOptions.java)
The Spark option constants to avoid typos and ensure reusability across Spark jobs.:
* Executor: Contains constants for Spark executor options.
* Column: Defines constants for column-related options.
* Common: Includes common options like header, schema inference, path, and format.
* CSV, Json, Parquet: Define format-specific options for CSV, JSON, and Parquet file formats.
* Mongo: Contains constants for MongoDB options.
* Arango: Defines options for ArangoDB options.
* Jdbc: Includes constants for JDBC options.
* Join: Defines constants for different join types.
* Kafka: Contains constants for Kafka options.
* Aws: Defines constants for AWS S3 options.

#### Miscellaneous
* [StringUtils.java](src/main/java/com/ksoot/spark/common/util/StringUtils.java): Utility methods for string manipulation.
* [JobConstants](src/main/java/com/ksoot/spark/common/util/JobConstants.java): Constants used across Spark jobs.
* [DateTimeUtils.java](src/main/java/com/ksoot/spark/common/util/DateTimeUtils.java) and [DateTimeFormatUtils.java](src/main/java/com/ksoot/spark/common/util/DateTimeFormatUtils.java): Utility methods for date and time manipulation and formatting.
* [ExecutionContext](src/main/java/com/ksoot/spark/common/util/ExecutionContext.java): A cleaner way to share and manage shared data or dependencies without using global variables or static references.

## Licence
Open source [**The MIT License**](http://www.opensource.org/licenses/mit-license.php)

## Authors and acknowledgment
[**Rajveer Singh**](https://www.linkedin.com/in/rajveer-singh-589b3950/), In case you find any issues or need any support, please email me at raj14.1984@gmail.com.
Give it a :star: on [Github](https://github.com/officiallysingh/spring-boot-spark-kubernetes) and a :clap: on [**medium.com**](https://officiallysingh.medium.com/spark-spring-boot-starter-e206def765b9) if you find it helpful.

## References
- [Apache Spark](https://spark.apache.org/docs/3.5.3)
- [Spark Data Sources](https://spark.apache.org/docs/latest/sql-data-sources.html)
- [Third Party Connectors](https://spark.apache.org/third-party-projects.html)
- [Spark MongoDB Connector](https://www.mongodb.com/docs/spark-connector/v10.4)
- [Spark ArangoDB Connector](https://docs.arangodb.com/3.13/develop/integrations/arangodb-datasource-for-apache-spark)
- [Spark Kafka Connector](https://spark.apache.org/docs/3.5.1/structured-streaming-kafka-integration.html)
- [Spark Streaming](https://spark.apache.org/docs/3.5.3/streaming-programming-guide.html)
- [Spark UI](https://spark.apache.org/docs/3.5.3/web-ui.html)
