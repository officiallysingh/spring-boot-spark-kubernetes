package com.ksoot.spark.launcher;

import com.ksoot.spark.dto.JobLaunchRequest;

public interface SparkJobLauncher {

  void startJob(final JobLaunchRequest jobLaunchRequest);

  void stopJob(final String jobCorrelationId);
}
