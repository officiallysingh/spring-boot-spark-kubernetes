# A Unified Framework for Building and Running Spark Jobs with Spring Boot and Deployment on Local and Minikube or Kubernetes

An innovative approach to implementing Spark Jobs with Spring Boot ecosystem, enabling developer-friendly environment.  
It integrates cross-cutting concerns as reusable libraries to minimize boilerplate code.  
Moreover, the framework supports one-click deployment of Spark jobs with RESTful APIs, making it a breeze to run jobs locally, on Minikube or Kubernetes.

## Introduction

[Apache Spark](https://spark.apache.org/docs/3.5.3/index.html) has become a powerful tool for processing large-scale data,
and when combined with Spring Boot, it offers a robust framework for building scalable, enterprise-grade data processing applications.
But for beginners it's tedious to get up and running.

# Installation
## Prerequisites
- Java 17
- [Scala 2.12.18](https://sdkman.io/install/)
- [Spark 3.5.3](https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz)
- [Minikube](https://minikube.sigs.k8s.io/docs/)
- IDE (IntelliJ, Eclipse or VS Code)
- Optional [Configure Formatter in intelliJ](https://github.com/google/google-java-format/blob/master/README.md#intellij-android-studio-and-other-jetbrains-ides), refer to [fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin) for details.

### Java
Recommended [sdkman](https://sdkman.io/install/) for managing Java, Scala installations.
Make sure `JAVA_HOME` set to Java 17 installation path and `PATH` variable contains entry for `$JAVA_HOME/bin`
Check Java version as follows. It should look like following, showing major version 17.
```shell
% java -version
openjdk version "17.0.12" 2024-07-16
OpenJDK Runtime Environment Temurin-17.0.12+7 (build 17.0.12+7)
OpenJDK 64-Bit Server VM Temurin-17.0.12+7 (build 17.0.12+7, mixed mode)
```

### Scala
Check Scala version as follows. It should look like following, showing scala version 2.12.18.
```shell
% scala -version
Scala code runner version 2.12.18 -- Copyright 2002-2023, LAMP/EPFL and Lightbend, Inc.
```

### Spark
Download and extract [spark-3.5.3-bin-hadoop3](https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz) on your machine and Set the following environment variables.
```shell
export SPARK_HOME="/<your directory>/spark-3.5.3-bin-hadoop3"
export SPARK_CONF_DIR=$SPARK_HOME/conf
export PATH="$SPARK_HOME/bin:$PATH"
```

## Environment setup
The demo jobs and `spark-job-service` need following services up and running.
- Make sure **Postgres** is running at `localhost:5432` with username `postgres` and password `admin`.  
  Create databases `spark_jobs_db` and `error_logs_db` if they do not exist.
- Make sure **MongoDB** running at `localhost:27017`.
- Make sure **ArangoDB** running at `localhost:8529` with `root` password as `admin`.
- Make sure **Kafka** running with bootstrap servers `localhost:9092`.
- Make sure **Kafka UI** running at `http://localhost:8100`. Create topics `job-stop-requests` and `error-logs` if they do not exist.

> [!IMPORTANT]  
> It is recommended to have port numbers same as mentioned above, otherwise you may need to change at multiple places i.e. in job's `application-local.yml`, `spark-job-service` application ymls and deployment yml etc.

### There are three ways to have required infrastructure up and running.
#### Local installations
All these services can be installed locally on your machine, and should be accessible at above-mentioned urls and credentials (wherever applicable).

#### Docker compose
* The [docker-compose.yml](docker-compose.yml) file defines the services and configurations to run required infrastructure in Docker. 
* Make sure Docker is running. 
* In Terminal go to project root `spring-boot-spark-kubernetes` and execute following command and Check if all services are running.
```shell
docker compose up -d
```
* Create databases `spark_jobs_db` and `error_logs_db` and Kafka topics `job-stop-requests` and `error-logs` if they do not exist.

> [!IMPORTANT]  
> While using docker compose make sure the required ports are free on your machine otherwise it will throw port busy error.

#### Minikube
The [infra-k8s-deployment.yml](infra-k8s-deployment.yml) file defines the services and configurations to run required infrastructure in Minikube.
* Set minikube cores to 4 and memory to 8GB atleast.
* Make sure docker is running and minikube is started.
* In Terminal go to project root `spring-boot-spark-kubernetes` and execute following command.
```shell
kubectl apply -f infra-k8s-deployment.yml
```
* Set default namespace to `ksoot` in minikube. You can always rollback it to default namespace.
```shell
kubectl config set-context --current --namespace=ksoot
```
* Check if all infra pods are running.
```shell
kubectl get pods
```
Output should look like below.
```shell
NAME                         READY   STATUS    RESTARTS   AGE
arango-65d6fff6c5-4bjwq      1/1     Running   0          6m16s
kafka-74c8d9579f-jmcr5       1/1     Running   0          6m16s
kafka-ui-797446869-9d8zw     1/1     Running   0          6m16s
mongo-6785c5cf8b-mtbk7       1/1     Running   0          6m16s
postgres-685b766f66-7dnsl    1/1     Running   0          6m16s
zookeeper-6fc87d48df-2t5pf   1/1     Running   0          6m16s
```
* Establish minikube tunnel to expose services of type `LoadBalancer` running in Minikube cluster to local machine.  
It creates a bridge between your local network and the Minikube cluster, making the required infrastructure accessible to local.
```shell
minikube tunnel
```
Keep it running in a separate terminal. Output should look like below.
```shell
✅  Tunnel successfully started

📌  NOTE: Please do not close this terminal as this process must stay alive for the tunnel to be accessible ...

🏃  Starting tunnel for service arango.
🏃  Starting tunnel for service kafka.
🏃  Starting tunnel for service kafka-ui.
🏃  Starting tunnel for service mongo.
🏃  Starting tunnel for service postgres.
🏃  Starting tunnel for service zookeeper.
```
* No need to create any databases or kafka topics required by applications as they are automatically created by [infra-k8s-deployment.yml](infra-k8s-deployment.yml).

# Framework Architecture
The proposed framework provides a comprehensive solution for building, running and deploying Spark jobs seamlessly.

### Features
Offers following features.
- **Job Launching**: Trigger Spark jobs via REST endpoint for deployment on local and kubernetes.
- **Job Termination**: Accept requests to stop running jobs via REST endpoint, though not a gauranteed method. You may need to kill the job manually if not terminated by this.
- **Job Monitoring**: Track job status, start and end time, duration taken, error messages if there is any, via REST endpoints.
- **Auto-configurations**: of Common components such as `SparkSession`, Job lifecycle listener and Connectors to read and write to various datasources.
- **Demo Jobs**: A [Spark Batch Job](spark-batch-daily-sales-report-job) and another [Spark Streaming Job](spark-stream-logs-analysis-job), to start with

### Components
The framework consists of following components. Refer to respective project's READMEs for details.
- [**spark-job-service**](spark-job-service/README.md): A Spring Boot application to launch Spark jobs and monitor their status.
- [**spring-boot-starter-spark**](https://github.com/officiallysingh/spring-boot-starter-spark): Spring boot starter for Spark for Csutomizable `SparkSession` auto-configurations.
- [**spark-job-commons**](spark-job-commons/README.md): A library to provide common Job components and utilities for Spark jobs.
- [**spark-batch-daily-sales-report-job**](spark-batch-daily-sales-report-job/README.md): A demo Spark Batch Job to generate daily sales report.
- [**spark-stream-logs-analysis-job**](spark-stream-logs-analysis-job/README.md): A demo Spark Streaming Job to analyze logs in real-time.

### Running Jobs Locally
- Spark Jobs can be run as Spring boot application locally in your favorite IDE. Refer to [daily-sales-job README](spark-batch-daily-sales-report-job/README.md#intellij-run-configurations) and [log-analysis-job README](spark-stream-logs-analysis-job/README.md#intellij-run-configurations).
- Spark Job can be Launched via REST API provided by `spark-job-service`. Refer to [spark-job-service README](spark-job-service/README.md#running-application-locally) for details.
- You can Run `spark-job-service` and Launch Jobs on Minikube or Kubernetes. Refer to [spark-job-service README](spark-job-service/README.md#running-application-locally) for details.

## Licence
Open source [**The MIT License**](http://www.opensource.org/licenses/mit-license.php)

## Authors and acknowledgment
[**Rajveer Singh**](https://www.linkedin.com/in/rajveer-singh-589b3950/), In case you find any issues or need any support, please email me at raj14.1984@gmail.com.
Please give me a :star: and a :clap: on [**medium.com**](https://officiallysingh.medium.com/spark-spring-boot-starter-e206def765b9) if you find it helpful.

## References
- [Apache Spark](https://spark.apache.org/docs/3.5.3/)
- [Spark Submit](https://spark.apache.org/docs/3.5.4/submitting-applications.html)
- [Running Spark on Kubernetes](https://spark.apache.org/docs/3.5.4/running-on-kubernetes.html)
- [Spring Cloud Task](https://spring.io/projects/spring-cloud-task)