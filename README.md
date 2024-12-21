# A Unified Framework for Building and Running Spark Jobs with Spring Boot and Deployment on Local and Minikube or Kubernetes

An innovative approach to implementing Spark Jobs with Spring Boot ecosystem, enabling developer-friendly environment.  
It integrates cross-cutting concerns as reusable libraries to minimize boilerplate code.  
Moreover, the framework supports one-click deployment of Spark jobs with RESTful APIs, making it a breeze to run jobs locally, on Minikube or Kubernetes.

## Introduction

[Apache Spark](https://spark.apache.org/docs/3.5.3/index.html) has become a powerful tool for processing large-scale data,
and when combined with Spring Boot, it offers a robust framework for building scalable, enterprise-grade data processing applications.
But for beginners it's tedious to get up and running.

# Framework Architecture

The proposed framework provides a comprehensive solution for managing Spark jobs through a RESTful interface, offering:
- Job Launching: Trigger Spark jobs and requests to stop running jobs via REST endpoints for deployment on local and kubernetes.
- Job Monitoring: Track job status, start and end time, duration taken, error messages if there is any.
- Auto-configurations: of Common components such as `SparkSession`, Job lifecycle listener and Connectors to read and write to various datasources.
- Demo Jobs: To start with a [Spark Batch Job](spark-batch-daily-sales-report-job) and another [Spark Streaming Job](spark-stream-logs-analysis-job)

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
