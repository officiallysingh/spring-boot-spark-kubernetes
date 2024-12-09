package com.ksoot.spark.churn;

import com.ksoot.spark.churn.conf.JobProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SparkPipelineExecutor {

  private final SparkSession sparkSession;

  private final JobProperties jobProperties;

  public void execute() {
    log.info("Spark Pipeline Executor Started at: " + LocalDateTime.now());
    final StopWatch stopWatch = StopWatch.createStarted();

    stopWatch.stop();
    log.info(
        "Spark Pipeline Executor completed at: {} successfully. Time taken: {}",
        LocalDateTime.now(),
        stopWatch.formatTime());

    // TODO: Handle exceptions
  }
}
