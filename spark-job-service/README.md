# Spark Job service
Spring boot service to start and stop spark jobs and Track job status, job duration, and error messages if there is any.  
It provides simple REST APIs for that, internally using `spark-submit` to start and kafka messaging to stop jobs respectively.

## Installation
### Prerequisites
- Java 17
- [Scala 2.12.18](https://sdkman.io/install/)
- [Spark 3.5.3](https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz)

Run [**`SparkJobService`**](src/main/java/com/ksoot/spark/SparkJobService.java) as Spring boot application in `local` profile.
> [!IMPORTANT]  
> Make sure environment variable `SPARK_HOME` is set to local spark installation.  
> Make sure environment variable `M2_REPO` is set to local maven repository.

### IntelliJ Run Configurations
* Got to main class [**`SparkJobService`**](src/main/java/com/ksoot/spark/SparkJobService.java) and Modify run
  configurations as follows.
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=local` to run in `local` profile. 
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=minikube` to run in `minikube` profile.
* Open swagger at http://localhost:8090/swagger-ui/index.html?urls.primaryName=Spark+Jobs
![Swagger](Swagger.png)

## API Reference
You can directly import the [**Postman Collection**](api-spec/Spark%20Job%20Service%20APIs.postman_collection.json)

### Spark Job Submit APIs

#### Start Spark Job

```http
POST /v1/spark-jobs/start
```

| Request Body    | Type                  | Description                                                                                          | Default                           | Required | 
|:----------------|:----------------------|:-----------------------------------------------------------------------------------------------------|:----------------------------------|:---------|
| `jobName`       | `String`              | Spark Job name, must be present in application.yml spark-submit.jobs                                 | -                                 | Yes      |
| `correlationId` | `String`              | Correlation id for each Job execution. Recommended but not mandatory to be unique for each execution | Random UUID, returned in response | Yes      |
| `sparkConfigs`  | `Map<String, Object>` | Runtime Spark conf properties for this job                                                           | Empty                             | Yes      |

**Supports three types of job launch requests. To support more jobs, Similarly write the corresponding Request class.**
- [DailySalesReportJobLaunchRequest](src/main/java/com/ksoot/spark/dto/DailySalesReportJobLaunchRequest.java)
- [LogsAnalysisJobLaunchRequest](src/main/java/com/ksoot/spark/dto/LogsAnalysisJobLaunchRequest.java)
- [SparkExampleJobLaunchRequest](src/main/java/com/ksoot/spark/dto/SparkExampleJobLaunchRequest.java)

Example curl to start [spark-batch-daily-sales-report-job](../spark-batch-daily-sales-report-job):
```curl
curl -X 'POST' \
  'http://localhost:8090/v1/spark-jobs/start' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "jobName": "daily-sales-report-job",
  "correlationId": "71643ba2-1177-4e10-a43b-a21177de1022",
  "sparkConfigs": {
    "spark.executor.instances": 4,
    "spark.driver.cores": 3
  },
  "month": "2024-11"
}'
```

Similary curl to start [spark-stream-logs-analysis-job](../spark-stream-logs-analysis-job):
```curl
curl -X 'POST' \
  'http://localhost:8090/v1/spark-jobs/start' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "jobName": "logs-analysis-job"
}'
```

#### Stop Spark Job

```http
POST /v1/spark-jobs/stop/{correlationId}
```

| Parameter       | Type     | Description        | Required |
|:----------------|:---------|:-------------------|:---------|
| `correlationId` | `String` | Job Correlation Id | Yes      |

### Spark Job Executions APIs

#### Get All Job Executions

```http
GET /v1/spark-jobs/executions
```

| Parameter | Type      | Description                                      | Default | Required |
|:----------|:----------|:-------------------------------------------------|:--------|:---------|
| `page`    | `Integer` | Zero-based page index (0..N)                     | 0       | No       |
| `size`    | `Integer` | The size of the page to be returned              | 10      | No       |
| `sort`    | `Sort`    | Sorting criteria in format: property,(asc\|desc) | -       | No       |

#### Get Job Executions By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}
```

| Parameter | Type      | Description                                      | Default | Required |
|:----------|:----------|:-------------------------------------------------|:--------|:---------|
| `jobName` | `String`  | Job name                                         | -       | Yes      |
| `page`    | `Integer` | Zero-based page index (0..N)                     | 0       | No       |
| `size`    | `Integer` | The size of the page to be returned              | 10      | No       |
| `sort`    | `Sort`    | Sorting criteria in format: property,(asc\|desc) | -       | No       |

#### Get Running Job Executions By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}/running
```

| Parameter | Type      | Description                                      | Default | Required |
|:----------|:----------|:-------------------------------------------------|:--------|:---------|
| `jobName` | `String`  | Job name                                         | -       | Yes      |
| `page`    | `Integer` | Zero-based page index (0..N)                     | 0       | No       |
| `size`    | `Integer` | The size of the page to be returned              | 10      | No       |
| `sort`    | `Sort`    | Sorting criteria in format: property,(asc\|desc) | -       | No       |

#### Get Latest Job Execution By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}/latest
```

| Parameter | Type     | Description | Required |
|:----------|:---------|:------------|:---------|
| `jobName` | `String` | Job name    | Yes      |

#### Get Latest Job Executions By Job Names

```http
GET /v1/spark-jobs/executions/latest
```

| Parameter  | Type           | Description | Required |
|:-----------|:---------------|:------------|:---------|
| `jobNames` | `List<String>` | Job Names   | Yes      |

#### Get Job Names

```http
GET /v1/spark-jobs/executions/job-names
```
No parameters required

#### Get Job Executions Count

```http
GET /v1/spark-jobs/executions/count
```
No parameters required

#### Get Job Executions Count By Job Name

```http
GET /v1/spark-jobs/executions/count/{jobName}
```

| Parameter | Type     | Description | Required |
| :-------- |:---------|:------------|:---------|
| `jobName` | `String` | Job name    | Yes      |

#### Get Running Job Executions Count

```http
GET /v1/spark-jobs/executions/count-running
```
No parameters required

#### Get Job Executions Count By Correlation Id

```http
GET /v1/spark-jobs/executions/count-by-correlation-id/{correlationId}
```

| Parameter       | Type     | Description        | Required |
|:----------------|:---------|:-------------------|:---------|
| `correlationId` | `String` | Job Correlation Id | Yes      |

#### Get Job Executions By Correlation Id

```http
GET /v1/spark-jobs/executions/by-correlation-id/{correlationId}
```

| Parameter       | Type      | Description                                      | Default | Required |
|:----------------|:----------|:-------------------------------------------------|:--------|:---------|
| `correlationId` | `String`  | Job Correlation Id                               | -       | Yes      |
| `page`          | `Integer` | Zero-based page index (0..N)                     | 0       | No       |
| `size`          | `Integer` | The size of the page to be returned              | 10      | No       |
| `sort`          | `Sort`    | Sorting criteria in format: property,(asc\|desc) | -       | No       |

## Implementation
* At its core it uses [spark-submit](https://spark.apache.org/docs/3.5.4/submitting-applications.html) to launch a Spark Job locally,  
on minikube or kubernetes in a unified way.  
* `spark-submit` command is derived with options coming through configurations in `application.yml` and Job start request Object.  
Refer to [SparkSubmitCommand Builder](src/main/java/com/ksoot/spark/launcher/SparkSubmitCommand.java) to see how the `spark-submit` command is built.
* [SparkJobLauncher.java](src/main/java/com/ksoot/spark/launcher/SparkJobLauncher.java) is the interface to launch a Spark Job.
Currently the only implementation is [SparkSubmitJobLauncher.java](src/main/java/com/ksoot/spark/launcher/SparkSubmitJobLauncher.java). However, similarl Job Launcher can be implemented for EMR also, placeholder at [SparkEmrJobLauncher.java](src/main/java/com/ksoot/spark/launcher/SparkEmrJobLauncher.java)

### Job Triggering
Currently Spark Job can be triggered by REST API.  
However, it can be enhanced to trigger spark jobs on arrival of Kafka messages, or Scheduler triggers or any other event also.
You just need to provide the corresponding implementation selected by some runtime strategy and call the intended `SparkJobLauncher` implementation to launch the job, as follows.
```java
this.sparkJobLauncher.startJob(jobLaunchRequest);   
```

### Configurations
#### Spark Configurations
All possible [Spark configurations](https://spark.apache.org/docs/3.5.3/configuration.html) can be set here.  
Remember you have similar Spark configurtions in individual Job also. Any configurtion set here will override the Job specific configuration.
```yaml
#---------- Spark configurations common to all Jobs -------------------------
spark:
  master: k8s://https://kubernetes.default.svc
  driver:
    memory: 2g
    cores: 2
  executor:
    instances: 2
    memory: 2g
    cores: 2
  default:
    parallelism: 32
#    extraJavaOptions: >
#      -DMONGODB_URL=${MONGODB_URL:mongodb://localhost:27017}
#      -DMONGO_FEATURE_DB=${MONGO_FEATURE_DB:feature-repo}
  kubernetes:
    namespace: spark
    authenticate.driver.serviceAccountName: spark
    driverEnv:
      SPARK_USER: spark
    #Always, Never, and IfNotPresent
#    container.image.pullPolicy: IfNotPresent
  submit.deployMode: cluster
```
#### Job Launcher Configurations
Following are Spark Job Launcher configurations.
```yaml
spark-launcher:
  #  spark-home: ${SPARK_HOME}
  capture-jobs-logs: true
  persist-jobs: true
  env:
    POSTGRES_URL: "jdbc:postgresql://postgres:5432/spark_jobs_db"
    KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  jobs:
    daily-sales-report-job:
      main-class-name: com.ksoot.spark.sales.DailySalesReportJob
      jar-file: local:///opt/spark/job-apps/spark-batch-daily-sales-report-job.jar
      env:
        MONGODB_URL: "mongodb://mongo:27017"
        ARANGODB_URL: "arango:8529"
      spark-config:
        spark.kubernetes.container.image: ${DAILY_SALES_REPORT_JOB_IMAGE:spark-batch-daily-sales-report-job:0.0.1}
    logs-analysis-job:
      main-class-name: com.ksoot.spark.loganalysis.LogAnalysisJob
      jar-file: local:///opt/spark/job-apps/spark-stream-logs-analysis-job.jar
      env:
        JDBC_URL: "jdbc:postgresql://postgres:5432"
      spark-config:
        spark.kubernetes.container.image: ${LOGS_ANALYSIS_JOB_IMAGE:spark-stream-logs-analysis-job:0.0.1}
```
**Description**
* `spark-home`: Spark installation directory. Default value is environment variable `SPARK_HOME`.
* `capture-jobs-logs`:- If set to `true` the triggered Job's logs are captures and displayed in this service's logs. Default value `false`. Not recommended in production.
* `persist-jobs`:- If set to `true` the Spring cloud task tracks the Jobs status in configured database. Default value `false`. Recommended in production.
* `env`:- Remember, there are environment variables defined in individual Job's `application.yml` such as `KAFKA_BOOTSTRAP_SERVERS`, `MONGODB_URL` etc.
The values of these environment variables to jobs can be provided from here. It should have environment variables that are common to all jobs.
* `jobs`:- A Map of each Job's configurations you wish to trigger from this service.
Each job has must be provided with some basic mandatory configurations and a few optional configurations.
  * `main-class-name`:- Fully qualified Main class name of the Spark Job. Its mandatory, as Spark needs to launch the Job by running its main class.
  * `jar-file`:- Jar file path of the Spark Job. This jar file is used in `spark-submit` command to launch the Job.
  * `env`:- Environment variables specific to this job. It overrides the common environment variables at `spark-launcher.env`.
  * `spark-config`:- Spark configurations specific to this job. It overrides the common Spark configurations at `spark`.
  You can unset any configuration coming from common spark configuration by setting it to `` (blank) here.

> [!IMPORTANT]  
> Job names `daily-sales-report-job` and `logs-analysis-job` given as keys in `spark-launcher.jobs` Map are used in Job start Request.
> Refer to below given curl to start [spark-batch-daily-sales-report-job](../spark-batch-daily-sales-report-job).  
> Note that `jobName` in Request body must match the key name in `spark-launcher.jobs` Map for this job, `daily-sales-report-job` in this case.
> It is recommended to have job name as `spring.application.name` in Job's `application.yml`.
> Also important to mention that the `sparkConfigs` in Request body will override the configurations in `spark-launcher.jobs.<jobName>.spark-config`, hence takes the highest precedence.
```curl
curl -X 'POST' \
  'http://localhost:8090/v1/spark-jobs/start' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "jobName": "daily-sales-report-job",
  "correlationId": "71643ba2-1177-4e10-a43b-a21177de1022",
  "sparkConfigs": {
    "spark.executor.instances": 4,
    "spark.driver.cores": 3
  },
  "month": "2024-11"
}'
```
#### Job Request class
* Each Job has a corresponding Request class to take the Job Start request as each job may have a different set of arguments, though some job arguments are common to all jobs.
* So a base abstract class [JobLaunchRequest.java](src/main/java/com/ksoot/spark/dto/JobLaunchRequest.java) is defined with common arguments.
* For each Job a Request class extending [JobLaunchRequest.java](src/main/java/com/ksoot/spark/dto/JobLaunchRequest.java) should be implemented.
* Refer to [DailySalesReportJobLaunchRequest](src/main/java/com/ksoot/spark/dto/DailySalesReportJobLaunchRequest.java) for [spark-batch-daily-sales-report-job](../spark-batch-daily-sales-report-job) and [LogsAnalysisJobLaunchRequest](src/main/java/ksoot/spark/dto/LogsAnalysisJobLaunchRequest.java) for [spark-stream-logs-analysis-job](../spark-stream-logs-analysis-job).
* Refer to [Jackson Inheritance](https://www.baeldung.com/jackson-inheritance#2-per-class-annotations) for help in implementing inheritance in request classes.

#### Spark submit
[SparkSubmitJobLauncher.java](src/main/java/com/ksoot/spark/launcher/SparkSubmitJobLauncher.java) launches the Job using `spark-submit` with options derived after merging common Spark configurations, job specific configurations in `application.yml` and Job request object.
Following is the `spark-submit` command generated for local deployment.
```shell
./bin/spark-submit --verbose --name daily-sales-report-job --class com.ksoot.spark.sales.DailySalesReportJob 
--conf spark.executor.memory=2g --conf spark.driver.memory=1g --conf spark.master=local --conf spark.driver.cores=3 
--conf spark.executor.cores=1 --conf spark.submit.deployMode=client --conf spark.executor.instances=4 
--conf spark.driver.extraJavaOptions="-Dspring.profiles.active=local -DSTATEMENT_MONTH=2024-11 -DCORRELATION_ID=71643ba2-1177-4e10-a43b-a21177de1022 -DPERSIST_JOB=true" 
 /Users/myusername/.m2/repository/com/ksoot/spark/spark-batch-daily-sales-report-job/0.0.1-SNAPSHOT/spark-batch-daily-sales-report-job-0.0.1-SNAPSHOT.jar
```
This command String is then passed to [spark-job-submit.sh](cmd/spark-job-submit.sh) on mac or linux and [spark-job-submit.bat](cmd/spark-job-submit.bat) on windows to execute the command.

## References
- [Spark Submit](https://spark.apache.org/docs/3.5.4/submitting-applications.html)
- [Running Spark on Kubernetes](https://spark.apache.org/docs/3.5.4/running-on-kubernetes.html)

