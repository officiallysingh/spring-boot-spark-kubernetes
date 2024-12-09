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
public class SparkExampleJobSubmitRequest extends JobSubmitRequest {

  private SparkExampleJobSubmitRequest(
      final String jobName, final Map<String, Object> sparkConfigs) {
    super(jobName, sparkConfigs);
  }

  @JsonCreator
  public static SparkExampleJobSubmitRequest of(
      @JsonProperty("jobName") final String jobName,
      @JsonProperty("sparkConfigs") final Map<String, Object> sparkConfigs) {
    return new SparkExampleJobSubmitRequest(jobName, sparkConfigs);
  }

  @Override
  public Map<String, String> jobVMOptions() {
    return Collections.emptyMap();
  }
}
