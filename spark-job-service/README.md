# Spark Job service
Spring boot service to start and stop spark jobs and Track job status, job duration, and error messages if there is any.  
It provides simple REST APIs for that, internally using `spark-submit` to start and kafka messaging to stop jobs respectively.

## Prerequisites
- Java 17
- [Scala 2.12.18](https://sdkman.io/install/)
- [Spark 3.5.3](https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz)

Run [**`SparkJobService`**](src/main/java/com/ksoot/spark/SparkJobService.java) as Spring boot application.
> [!IMPORTANT]  
> Make sure environment variable `SPARK_HOME` is set to local spark installation.  
> Make sure environment variable `M2_REPO` is set to local maven repository.

## IntelliJ Run Configurations
* Got to main class [**`SparkJobService`**](src/main/java/com/ksoot/spark/SparkJobService.java) and Modify run
  configurations as follows.
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=local` to run in `local` profile. 
* Go to `Modify options`, click on `Add VM options` and set the value as `-Dspring.profiles.active=minikube` to run in `minikube` profile.
* Open swagger in browser at http://localhost:8090/swagger-ui/index.html?urls.primaryName=Spark+Jobs

## API Reference
You can directly import the [Postman Collection](api-spec/Spark Job Service APIs.postman_collection.json)

### Spark Job Submit APIs

#### Start Spark Job

```http
POST /v1/spark-jobs/start
```

| Request Body | Type | Description | Default                           |Required | 
| :----------- | :--- | :---------- | :----------------------------------| :------- |
| `jobName` | `string` | Spark Job name, must be present in application.yml spark-submit.jobs | -                                 | Yes |
| `correlationId` | `string` | Unique correlation id for each Job execution | Random UUID, returned in response | Yes |
| `sparkConfigs` | `object` | Runtime Spark conf properties for this job | Empty | Yes |

**Supports three types of job launch requests. To support more jobs, Similarly write the corresponding Request class.**
- [DailySalesReportJobLaunchRequest](src/main/java/com/ksoot/spark/dto/DailySalesReportJobLaunchRequest.java)
- [LogsAnalysisJobLaunchRequest](src/main/java/com/ksoot/spark/dto/LogsAnalysisJobLaunchRequest.java)
- [SparkExampleJobLaunchRequest](src/main/java/com/ksoot/spark/dto/SparkExampleJobLaunchRequest.java)



#### Stop Spark Job

```http
POST /v1/spark-jobs/stop/{correlationId}
```

| Parameter | Type | Description | Required |
| :-------- | :--- | :---------- | :------- |
| `correlationId` | `string` | Job Correlation Id | Yes |

### Spark Job Executions APIs

#### Get All Job Executions

```http
GET /v1/spark-jobs/executions
```

| Parameter | Type | Description | Default | Required |
| :-------- | :--- | :---------- | :------ | :------- |
| `page` | `integer` | Zero-based page index (0..N) | 0 | No |
| `size` | `integer` | The size of the page to be returned | 10 | No |
| `sort` | `array` | Sorting criteria in format: property,(asc\|desc) | - | No |

#### Get Job Executions By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}
```

| Parameter | Type | Description | Default | Required |
| :-------- | :--- | :---------- | :------ | :------- |
| `jobName` | `string` | Job name | - | Yes |
| `page` | `integer` | Zero-based page index (0..N) | 0 | No |
| `size` | `integer` | The size of the page to be returned | 10 | No |
| `sort` | `array` | Sorting criteria in format: property,(asc\|desc) | - | No |

#### Get Running Job Executions By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}/running
```

| Parameter | Type | Description | Default | Required |
| :-------- | :--- | :---------- | :------ | :------- |
| `jobName` | `string` | Job name | - | Yes |
| `page` | `integer` | Zero-based page index (0..N) | 0 | No |
| `size` | `integer` | The size of the page to be returned | 10 | No |
| `sort` | `array` | Sorting criteria in format: property,(asc\|desc) | - | No |

#### Get Latest Job Execution By Job Name

```http
GET /v1/spark-jobs/executions/{jobName}/latest
```

| Parameter | Type | Description | Required |
| :-------- | :--- | :---------- | :------- |
| `jobName` | `string` | Job name | Yes |

#### Get Latest Job Executions By Job Names

```http
GET /v1/spark-jobs/executions/latest
```

| Parameter | Type | Description | Required |
| :-------- | :--- | :---------- | :------- |
| `jobNames` | `array` | Job Names | Yes |

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

| Parameter | Type | Description | Required |
| :-------- | :--- | :---------- | :------- |
| `jobName` | `string` | Job name | Yes |

#### Get Running Job Executions Count

```http
GET /v1/spark-jobs/executions/count-running
```
No parameters required

#### Get Job Executions Count By Correlation Id

```http
GET /v1/spark-jobs/executions/count-by-correlation-id/{correlationId}
```

| Parameter | Type | Description | Required |
| :-------- | :--- | :---------- | :------- |
| `correlationId` | `string` | Job Correlation Id | Yes |

#### Get Job Executions By Correlation Id

```http
GET /v1/spark-jobs/executions/by-correlation-id/{correlationId}
```

| Parameter | Type | Description | Default | Required |
| :-------- | :--- | :---------- | :------ | :------- |
| `correlationId` | `string` | Job Correlation Id | - | Yes |
| `page` | `integer` | Zero-based page index (0..N) | 0 | No |
| `size` | `integer` | The size of the page to be returned | 10 | No |
| `sort` | `array` | Sorting criteria in format: property,(asc\|desc) | - | No |
