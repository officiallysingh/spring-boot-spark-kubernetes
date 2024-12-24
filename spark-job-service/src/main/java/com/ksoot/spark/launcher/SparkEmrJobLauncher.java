package com.ksoot.spark.launcher;

import com.ksoot.spark.conf.SparkLauncherProperties;
import com.ksoot.spark.dto.JobLaunchRequest;
import java.util.Properties;

// TODO: Implement to submit job on EMR
public class SparkEmrJobLauncher extends AbstractSparkJobLauncher {

  protected SparkEmrJobLauncher(
      final Properties sparkProperties, final SparkLauncherProperties sparkLauncherProperties) {
    super(sparkProperties, sparkLauncherProperties);
  }

  @Override
  public void startJob(JobLaunchRequest jobLaunchRequest) {}

  @Override
  public void stopJob(String jobCorrelationId) {}
}
