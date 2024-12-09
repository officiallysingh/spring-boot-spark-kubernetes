package com.ksoot.spark.conf;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
@ConfigurationProperties(prefix = "spark-submit")
public class SparkSubmitProperties {

  /** Spark installation path. */
  @NotEmpty private String sparkHome = System.getenv("SPARK_HOME");

  /** Whether to collect the spark submitted Job's logs in this service. Default: false */
  private boolean captureJobsLogs = false;

  /** Whether to persist Jobs status in Database. Default: false */
  private boolean persistJobs = false;

  private Map<@NotEmpty String, @NotNull SparkJobProperties> jobs = new LinkedHashMap<>();
}
