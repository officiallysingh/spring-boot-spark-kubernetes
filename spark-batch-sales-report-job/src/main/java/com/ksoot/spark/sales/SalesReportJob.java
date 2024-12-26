package com.ksoot.spark.sales;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@EnableTask
@EnableKafka
@SpringBootApplication
public class SalesReportJob {

  @Value("${ksoot.hadoop-dll:null}")
  private String hadoopDll;

  public static void main(String[] args) {
    SpringApplication.run(SalesReportJob.class, args);
  }

  @Autowired private DataPopulator dataPopulator;

  @PostConstruct
  public void init() {
    log.info("Initializing SalesReportJob ...");

    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      System.load(this.hadoopDll);
    }

    this.dataPopulator.populateData();
  }

  @Bean
  public ApplicationRunner applicationRunner(final SparkPipelineExecutor sparkPipelineExecutor) {
    return new SparkStatementJobRunner(sparkPipelineExecutor);
  }

  @Slf4j
  static class SparkStatementJobRunner implements ApplicationRunner {

    private final SparkPipelineExecutor sparkPipelineExecutor;

    public SparkStatementJobRunner(final SparkPipelineExecutor sparkPipelineExecutor) {
      this.sparkPipelineExecutor = sparkPipelineExecutor;
    }

    @Override
    public void run(final ApplicationArguments args) {
      this.sparkPipelineExecutor.execute();
    }
  }
}
