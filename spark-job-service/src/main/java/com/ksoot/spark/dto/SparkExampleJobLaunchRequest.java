package com.ksoot.spark.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Map;
import lombok.*;

@Getter
@ToString(callSuper = true)
@Valid
@JsonTypeName("spark-example")
// Should be Immutable
public class SparkExampleJobLaunchRequest extends JobLaunchRequest {

  private SparkExampleJobLaunchRequest(
      final String jobName, final Map<String, Object> sparkConfigs) {
    super(jobName, sparkConfigs);
  }

  @JsonCreator
  public static SparkExampleJobLaunchRequest of(
      @JsonProperty("jobName") final String jobName,
      @JsonProperty("sparkConfigs") final Map<String, Object> sparkConfigs) {
    return new SparkExampleJobLaunchRequest(jobName, sparkConfigs);
  }

  @Override
  public Map<String, String> jobArgs() {
    return Collections.emptyMap();
  }
}
