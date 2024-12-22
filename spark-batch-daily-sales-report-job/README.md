# Daily Sales Report Job
Demo Spark Batch job implemented as Spring Cloud Task

Run [**`DailySalesReportJob`**](src/main/java/com/ksoot/spark/sales/DailySalesReportJob.java) as Spring boot application.

> [!IMPORTANT]  
> Run in active profile `local` locally.  
> Set VM argument `--add-exports java.base/sun.nio.ch=ALL-UNNAMED`

## Installation
### Prerequisites
- Java 17
- Docker
- Maven

### Environment setup
- Make sure **Postgres** is running at `dbc:postgresql://localhost:5432` with username `postgres` and password `admin`.  
  Create database `spark_jobs_db` if it does not exist.
- Make sure **ArangoDB** running at `localhost:8529` with `root` password as `admin`.
- Make sure **MongoDB** running at `mongodb://localhost:27017`.
- Make sure **Kafka** running with bootstrap servers `localhost:9092`
- Make sure **Kafka UI** running at `http://localhost:8100`. Create topic `job-stop-requests` if it does not exist.

> [!IMPORTANT]  
> If any port or credentials are different from above mentioned then do the respective changes in [application-local.yml](src/main/resources/config/application-local.yml).  

Either above infrastructure is up and running as local installations in your system, or use **docker compose** using [docker-compose.yml](../docker-compose.yml) to run the above-mentioned infrastructure.
In Terminal go to project root `spring-boot-spark-kubernetes` and execute following command.
```shell
docker compose up -d
```
> [!IMPORTANT]  
> While using docker compose make sure the required ports are free on your machine otherwise it will throw port busy error.

### IntelliJ Run Configurations
* Got to main class [**`DailySalesReportJob`**](src/main/java/com/ksoot/spark/sales/DailySalesReportJob.java) and Modify run
  configurations as follows.
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=local` to run in `local` profile.
* Go to `Modify options`, click on `Add VM options` and set the value as `--add-exports java.base/sun.nio.ch=ALL-UNNAMED`  
  to avoid exception `Factory method 'sparkSession' threw exception with message: class org.apache.spark.storage.StorageUtils$ (in unnamed module @0x2049a9c1) cannot access class sun.nio.ch.DirectBuffer (in module java.base) because module java.base does not export sun.nio.ch to unnamed module @0x2049a9c1`.
* Go to `Modify options` and make sure `Add dependencies with "provided" scope to classpath` is checked.

## Description
- This Spark Job is implemented as [Spring Cloud Task](https://spring.io/projects/spring-cloud-task).
- On application startup it populates sample transaction and master data into respective database collections for last 6 months.  
Refer to [DataPopulator](src/main/java/com/ksoot/spark/sales/DataPopulator.java) for details.
- It expects an argument `month` with default value as current month if not specified and generates sales report for this month.
- It reads sales transaction data from MongoDB database `sales_db`, collection `sales` as a Spark `Dataset<Row>`
- Filters this dataset for given month.
- Then joins it with Product master data fetched from ArangoDB database `products_db`, collection `products`  
to produce the result dataset.
- The result dataset is then written into MongoDB collection named `sales_report_<input month>`.  
For example if input month is `2024-11` then output will be written into collection `sales_report_2024_11`.
- For details refer to [SparkPipelineExecutor](src/main/java/com/ksoot/spark/sales/SparkPipelineExecutor.java)
- Following is the Spark pipeline code
```java
public void execute() {
    log.info("Generating Daily sales report for month: {}", this.jobProperties.getMonth());

    Dataset<Row> salesDataset = this.mongoConnector.read("sales");
    SparkUtils.logDataset("Sales Dataset", salesDataset);

    final String statementMonth = this.jobProperties.getMonth().toString();
    // Convert `timestamp` to date and calculate daily sales amount
    Dataset<Row> aggregatedSales =
        salesDataset
            .filter(col("timestamp").startsWith(statementMonth))
            .withColumn("date", col("timestamp").substr(0, 10)) // Extract date part
            .withColumn(
                "sale_amount",
                col("price")
                    .cast(DataTypes.DoubleType)
                    .multiply(col("quantity").cast(DataTypes.IntegerType))) // Calculate sale_amount
            .groupBy("product_id", "date")
            .agg(sum("sale_amount").alias("daily_sale_amount"));

    Dataset<Row> productsDataset = this.arangoConnector.readAll("products");
    SparkUtils.logDataset("Products Dataset", salesDataset);
    productsDataset =
        productsDataset.select(col("_key").as("product_id"), col("name").as("product_name"));

    // Join with the product details dataset
    Dataset<Row> dailySalesReport =
        aggregatedSales
            .join(
                productsDataset,
                aggregatedSales.col("product_id").equalTo(productsDataset.col("product_id")))
            .select(
                productsDataset.col("product_name").as("product"),
                aggregatedSales.col("date"),
                aggregatedSales.col("daily_sale_amount").alias("sale"))
            .orderBy(col("product_name"), col("date"));

    // Show the final result
    SparkUtils.logDataset("Daily Sales report", dailySalesReport);

    final String salesReportCollection = "sales_report_" + statementMonth.replace('-', '_');
    //    this.fileConnector.write(dailySalesReport); // For testing
    this.mongoConnector.write(dailySalesReport, salesReportCollection);
  }
```

## Configurations
You can find the default Job configuration in [application.yml](src/main/resources/config/application.yml) as follows.

```yaml
ksoot:
#  Applicable only while running on Windows machine
  hadoop-dll: ${HOME}/hadoop-3.0.0/bin/hadoop.dll
  job:
    month: ${STATEMENT_MONTH:}
    correlation-id: ${CORRELATION_ID:${spring.application.name}-1}
    persist: ${PERSIST_JOB:false}
    job-stop-topic: ${JOB_STOP_TOPIC:job-stop-requests}
  connector:
    save-mode: Append
    output-mode: Update
    mongo-options:
      url: ${MONGODB_URL:mongodb://localhost:27017}
      database: ${MONGODB_DATABASE:sales_db}
    arango-options:
      endpoints: ${ARANGODB_URL:localhost:8529}
      database: ${ARANGODB_DATABASE:products_db}
      username: ${ARANGODB_USER:root}
      password: ${ARANGODB_PASSWORD:admin}
      ssl-enabled: false
      ssl-cert-value: ""
      cursor-ttl: PT5M # 5 minutes, see the ISO 8601 standard for java.time.Duration String patterns
    file-options:
      format: csv
      header: true
      path: ${SPARK_OUTPUT_PATH:spark-space/output}
      merge: true
    kafka-options:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      fail-on-data-loss: ${KAFKA_FAIL_ON_DATA_LOSS:false}
```

> [!IMPORTANT]  
> Configurations in [application.yml](src/main/resources/config/application.yml) are supposed to be production defualt. While running locally,  
> you can override any configuration in [application-local.yml](src/main/resources/config/application-local.yml)
