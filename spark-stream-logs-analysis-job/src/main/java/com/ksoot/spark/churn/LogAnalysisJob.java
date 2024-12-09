package com.ksoot.spark.churn;

import com.ksoot.spark.churn.conf.JobProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableTask
@SpringBootApplication
@EnableConfigurationProperties(JobProperties.class)
public class LogAnalysisJob {

  public static void main(String[] args) {
    SpringApplication.run(LogAnalysisJob.class, args);
  }

  @PostConstruct
  public void init() {
    log.info("Initializing SparkWordCountJob ...");
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
