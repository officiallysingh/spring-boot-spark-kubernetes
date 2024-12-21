package com.ksoot.spark.loganalysis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableTask
@EnableKafka
@EnableScheduling
@SpringBootApplication
public class LogAnalysisJob {

  @Value("${ksoot.hadoop-dll:null}")
  private String hadoopDll;

  public static void main(String[] args) {
    SpringApplication.run(LogAnalysisJob.class, args);
  }

  @PostConstruct
  public void init() {
    log.info("Initializing LogAnalysisJob ...");
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      System.load(this.hadoopDll);
    }
  }

  @Bean
  public ApplicationRunner applicationRunner(final SparkPipelineExecutor sparkPipelineExecutor) {
    return new LogAnalysisJobRunner(sparkPipelineExecutor);
  }

  @Slf4j
  static class LogAnalysisJobRunner implements ApplicationRunner {

    private final SparkPipelineExecutor sparkPipelineExecutor;

    public LogAnalysisJobRunner(final SparkPipelineExecutor sparkPipelineExecutor) {
      this.sparkPipelineExecutor = sparkPipelineExecutor;
    }

    @Override
    public void run(final ApplicationArguments args) {
      this.sparkPipelineExecutor.execute();
    }
  }
}
