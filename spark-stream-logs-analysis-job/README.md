# Log Analysis Job
Demo **Spark Streaming job** implemented as Spring Cloud Task

Run [**`LogAnalysisJob`**](src/main/java/com/ksoot/spark/loganalysis/LogAnalysisJob.java) as Spring boot application.

> [!IMPORTANT]  
> Run in active profile `local` locally.  
> Set VM argument `--add-exports java.base/sun.nio.ch=ALL-UNNAMED` to avoid exception `Factory method 'sparkSession' threw exception with message: class org.apache.spark.storage.StorageUtils$ (in unnamed module @0x2049a9c1) cannot access class sun.nio.ch.DirectBuffer (in module java.base) because module java.base does not export sun.nio.ch to unnamed module @0x2049a9c1`.

## Installation
### Prerequisites
- Java 17
- Docker
- Maven

### Environment setup
- Make sure **Postgres** is running at `localhost:5432` with username `postgres` and password `admin`.  
  Create database `spark_jobs_db` and `error_logs_db` if they do not exist.
- Make sure **Kafka** running with bootstrap servers `localhost:9092`
- Make sure **Kafka UI** running at `http://localhost:8100`. Create topics `job-stop-requests` and `error-logs` if they do not exist.

> [!IMPORTANT]  
> If any port or credentials are different from above mentioned then override respective configurations in [application-local.yml](src/main/resources/config/application-local.yml).

Either above infrastructure is up and running as local installations in your system, or use **docker compose** using [docker-compose.yml](../docker-compose.yml) to run required infrastructure.
In Terminal go to project root `spring-boot-spark-kubernetes` and execute following command.
```shell
docker compose up -d
```
> [!IMPORTANT]  
> While using docker compose make sure the required ports are free on your machine otherwise it will throw port busy error.

### IntelliJ Run Configurations
* Got to main class [**`LogAnalysisJob`**](src/main/java/com/ksoot/spark/loganalysis/LogAnalysisJob.java) and Modify run
  configurations as follows.
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=local` to run in `local` profile.
* Go to `Modify options`, click on `Add VM options` and set the value as `--add-exports java.base/sun.nio.ch=ALL-UNNAMED`  
  to avoid exception `Factory method 'sparkSession' threw exception with message: class org.apache.spark.storage.StorageUtils$ (in unnamed module @0x2049a9c1) cannot access class sun.nio.ch.DirectBuffer (in module java.base) because module java.base does not export sun.nio.ch to unnamed module @0x2049a9c1`.
* Go to `Modify options` and make sure `Add dependencies with "provided" scope to classpath` is checked.

## Spark Job implementation
### Spark Configurations
For Spark auto-configurations [spring-boot-starter-spark](https://github.com/officiallysingh/spring-boot-starter-spark) is used by adding the following dependency.
```xml
<dependency>
    <groupId>io.github.officiallysingh</groupId>
    <artifactId>spring-boot-starter-spark</artifactId>
    <version>1.2</version>
</dependency>
```
to avail the following features.
- Spark dependencies compatible with Spring boot 3+.
- Customizable `SparkSession` bean auto-configured.
- Enables auto-completion assistance for Spark configuration properties in `application.yml`
- Any Spark configuration can be set in `application.yml` as follows.
```yaml
spark:
  ui:
    enabled: true
  streaming:
    stopGracefullyOnShutdown: true
  sql:
    streaming:
      checkpointLocation: ${CHECKPOINT_LOCATION:spark-space/checkpoints}
      forceDeleteTempCheckpointLocation: true
    adaptive:
      enabled: true
  checkpoint:
    compress: true
```

### Spark Pipeline
- This Spark Job is implemented as [Spring Cloud Task](https://spring.io/projects/spring-cloud-task).
- Randomly generated error logs are written to kafka topic `error-logs`, for details refer to [LogsGenerator](src/main/java/com/ksoot/spark/loganalysis/LogsGenerator.java).
- Spark streaming pipeline connects to kafka topic `error-logs` and read the text log messages as stream in `Dataset<Row>`.
- Then it filters the data with log leve `ERROR`.
- Then it writes the output to Postgres database `error_logs_db` table `error_logs` as stream continuously.
- Application starts and awaits on Spark `DataStreamWriter` in a `Retryable` wrapper to make it fault-tolerant.
- For details refer to [SparkPipelineExecutor](src/main/java/com/ksoot/spark/loganalysis/SparkPipelineExecutor.java)
- Following is the Spark pipeline code
```java

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
  this.startAndAwaitRetryableStream(logsStreamWriter);
  //    this.taskExecutor.execute(() -> this.startAndAwaitRetryableStream(logsStreamWriter));
}

@Retryable(
        retryFor = {StreamRetryableException.class},
        maxAttempts = Integer.MAX_VALUE,
        backoff = @Backoff(delay = 5000)) // Delay of 5 seconds, with unlimited retry attempts
private void startAndAwaitRetryableStream(final DataStreamWriter<?> dataStreamWriter) {
  try {
    final StreamingQuery streamingQuery = dataStreamWriter.start();
    this.sparkExecutionManager.addStreamingQuery(streamingQuery);
    streamingQuery.awaitTermination();
  } catch (final TimeoutException | StreamingQueryException e) {
    log.error(
            "Exception in Spark stream: {}. Will retry to recover from error after 5 seconds",
            e.getMessage());
    throw new StreamRetryableException("Exception in spark streaming", e);
  }
}
```

> [!IMPORTANT]  
> In case of recoverable errors always wrap exception in `StreamRetryableException` and throw it,  
> so that Job does not exit, but keep on retrying to connect to recover from transient errors such as kafka connectivity issues.
> But in some error situations you may want the job to fail.


### Configurations
You can find the default Job configurations in [application.yml](src/main/resources/config/application.yml) as follows.

```yaml
ksoot:
  #  Applicable only while running on Windows machine, replace ${HOME} with your directory
  hadoop-dll: ${HOME}/hadoop-3.0.0/bin/hadoop.dll
  job:
    correlation-id: ${CORRELATION_ID:${spring.application.name}-1}
    persist: ${PERSIST_JOB:false}
    job-stop-topic: ${JOB_STOP_TOPIC:job-stop-requests}
  connector:
    save-mode: Append
    output-mode: Update
    jdbc-options:
      url: ${JDBC_URL:jdbc:postgresql://localhost:5432}
      database: ${JDBC_DB:error_logs_db}
      username: ${JDBC_USER:postgres}
      password: ${JDBC_PASSWORD:admin}
      batchsize: ${JDBC_BATCH_SIZE:1000}
      isolation-level: ${JDBC_ISOLATION_LEVEL:READ_UNCOMMITTED}
    file-options:
      format: csv
      header: true
      path: ${SPARK_OUTPUT_PATH:spark-space/output}
      merge: true
    kafka-options:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      topic: ${KAFKA_ERROR_LOGS_TOPIC:error-logs}
      fail-on-data-loss: ${KAFKA_FAIL_ON_DATA_LOSS:false}
```

**Description**
* `ksoot.hadoop-dll`:- To run Spark Job on Windows machine, you need to download [winutils](https://github.com/steveloughran/winutils/tree/master/hadoop-3.0.0/bin), extract and set the path in this config.
* `ksoot.job.correlation-id`:- The Job Correlation Id used to track job status or stop a running job from [spark-job-service](spark-job-service) REST API.
  Its value is set to `spring.cloud.task.external-execution-id`. It is recommended but not required to be unique for each Job execution.
* `ksoot.job.persist`:- If set to true the Job status is tracked in Postgres database `spark_job_db`, table `task_execution`.  
  Its value is set to `spring.cloud.task.initialize-enabled`.
* `ksoot.job.job-stop-topic`:- The kafka topic name where the job listens for requests to Stop the long-running Job.  
  Expected message content is `correlation-id` of Job execution for which the termination is requested.  
  Multiple Job executions could be running at a time, the running jobs that match the correlation id received in kafka message are terminated.
* `ksoot.job.connector`:- Configurations for various Spark connectors.

> [!IMPORTANT]  
> Configurations in [application.yml](src/main/resources/config/application.yml) are supposed to be production defaults.
> While running locally, you can override any configuration in [application-local.yml](src/main/resources/config/application-local.yml)

For example to generate sales report for a particular month set `ksoot.job.month` to respective month as follows.
```yaml
ksoot:
  connector:
    save-mode: Append
    output-mode: Update
    file-options:
      format: parquet
    kafka-options:
      fail-on-data-loss: true
```

### Error Handling
- In case of any uncaught exceptions, the Job will exit with non-zero exit code.
- Following best practices no need to create any custom exception classes.  
  [JobProblem.java](../spark-job-commons/src/main/java/com/ksoot/spark/common/error/JobProblem.java) can be used to throw exceptions as follows.

```java
try {
    // Some file reading code
} catch (final IOException e) {
  throw JobProblem.of("IOException while listing file by reading from aws").cause(e).build();
}
```
- The job will exit and error message will be logged.
- For scenarios where you don't want the job to exit, Catch and Handle exceptions properly.

## Spring Cloud Task database
When `spring.cloud.task.initialize-enabled` is set to true, Spring cloud task initializes its database schema in Postgres database `spark_jobs_db`.

### Task Executions
If `ksoot.job.persist` is set to `true` then Job status log is tracked as follows in database `spark_jobs_db` table `task_execution`.  
This is a Spring Cloud Task feature, for details refer to [documentation](https://docs.spring.io/spring-cloud-task/reference/features.html)

| task_execution_id | start_time                 | end_time                   | task_name               | exit_code | exit_message | error_message                                                                            | last_updated                  | external_execution_id                | parent_execution_id   |
|-------------------|----------------------------|----------------------------|-------------------------|-----------|--------------|------------------------------------------------------------------------------------------|-------------------------------|--------------------------------------|-----------------------|
| 1                 | 2024-12-21 13:15:52.849979 | 2024-12-21 13:17:57.044739 | logs-analysis-job       | 1         | Failed       | com.ksoot.spark.common.util.StreamRetryableException: Exception in spark streaming ..... | 2024-12-21 13:17:57.058191    | logs-analysis-job-1                  | NULL                  |
| 2                 | 2024-12-21 13:38:09.022006 | 2024-12-21 13:38:39.857139 | logs-analysis-job       | 1         | Failed       | com.ksoot.spark.common.util.StreamRetryableException: Exception in spark streaming ..... | 2024-12-21 13:38:39.86834     | logs-analysis-job-1                  | NULL                  |
| 3                 | 2024-12-21 13:38:48.227212 | 2024-12-21 13:38:51.117856 | daily-sales-report-job  | 0         | Completed    | NULL                                                                                     | 2024-12-21 13:38:51.54849     | daily-sales-report-job-1             | NULL                  |
| 4                 | 2024-12-21 13:40:22.491883 | 2024-12-21 13:40:25.539387 | daily-sales-report-job  | 0         | Completed    | NULL                                                                                     | 2024-12-21 13:40:25.827281    | daily-sales-report-job-1             | NULL                  |
| 5                 | 2024-12-21 13:40:42.24037  | 2024-12-21 13:41:53.290863 | logs-analysis-job       | 0         | Terminated   | NULL                                                                                     | 2024-12-21 13:41:53.789612    | logs-analysis-job-1                  | NULL                  |
| 6                 | 2024-12-21 13:45:09.334699 | 2024-12-21 13:45:50.076483 | logs-analysis-job       | 0         | Terminated   | NULL                                                                                     | 2024-12-21 13:46:32.726645    | logs-analysis-job-1                  | NULL                  |
| 7                 | 2024-12-21 16:37:10.097431 | 2024-12-21 16:37:13.224737 | daily-sales-report-job  | 0         | Completed    | NULL                                                                                     | 2024-12-21 16:37:13.395288    | 71643ba2-1177-4e10-a43b-a21177de1022 | NULL                  |
| 8                 | 2024-12-21 16:39:03.951104 | 2024-12-21 16:39:06.977208 | daily-sales-report-job  | 0         | Completed    | NULL                                                                                     | 2024-12-21 16:39:07.227733    | 71643ba2-1177-4e10-a43b-a21177de1022 | NULL                  |
| 9                 | 2024-12-21 18:41:13.141188 | 2024-12-21 18:41:16.363157 | daily-sales-report-job  | 0         | Completed    | NULL                                                                                     | 2024-12-21 18:41:16.408378    | 71643ba2-1177-4e10-a43b-a21177de1022 | NULL                  |
