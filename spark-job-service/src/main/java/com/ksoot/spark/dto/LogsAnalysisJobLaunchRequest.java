package com.ksoot.spark.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.*;

@Getter
@ToString(callSuper = true)
@Valid
@JsonTypeName("logs-analysis-job")
// Should be Immutable
public class LogsAnalysisJobLaunchRequest extends JobLaunchRequest {

  private LogsAnalysisJobLaunchRequest(
      final String jobName, final Map<String, Object> sparkConfigs) {
    super(jobName, sparkConfigs);
  }

  @JsonCreator
  public static LogsAnalysisJobLaunchRequest of(
      @JsonProperty("jobName") final String jobName,
      @JsonProperty("sparkConfigs") final Map<String, Object> sparkConfigs) {
    return new LogsAnalysisJobLaunchRequest(jobName, sparkConfigs);
  }

  @Override
  public Map<String, String> jobArgs() {
    return Map.of();
  }
}
