package com.ksoot.spark.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@ToString
@Valid
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "jobName",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SparkExampleJobSubmitRequest.class, name = "spark-example"),
  @JsonSubTypes.Type(
      value = DailySalesReportJobSubmitRequest.class,
      name = "daily-sales-report-job"),
  @JsonSubTypes.Type(value = LogsAnalysisJobSubmitRequest.class, name = "logs-analysis-job")
})
public abstract class JobSubmitRequest {

  @Schema(
      description = "Spark Job name, must be present in application.yml spark-submit.jobs",
      example = "spark-example")
  @NotEmpty
  protected final String jobName;

  @Schema(
      description = "Unique correlation id for each Job execution.",
      example = "71643ba2-1177-4e10-a43b-a21177de1022")
  @NotEmpty
  protected final String correlationId;

  /**
   * Runtime Spark conf properties for this job.
   *
   * @see <a
   *     href="https://spark.apache.org/docs/3.5.3/configuration.html#available-properties">Spark
   *     configurations</a>
   * @see <a
   *     href="https://spark.apache.org/docs/3.5.3/running-on-kubernetes.html#configuration">Spark
   *     Kubernetes configurations</a>
   */
  @Schema(
      description = "Runtime Spark conf properties for this job.",
      example =
          """
            {
              "spark.executor.instances": 4,
              "spark.driver.cores": 3
            }
          """)
  @NotNull
  protected final Map<String, Object> sparkConfigs;

  protected JobSubmitRequest(final String jobName) {
    this(jobName, null, null);
  }

  protected JobSubmitRequest(final String jobName, final Map<String, Object> sparkConfigs) {
    this(jobName, null, sparkConfigs);
  }

  protected JobSubmitRequest(final String jobName, final String correlationId) {
    this(jobName, correlationId, null);
  }

  protected JobSubmitRequest(
      final String jobName, final String correlationId, final Map<String, Object> sparkConfigs) {
    this.jobName = jobName;
    this.correlationId =
        StringUtils.isNotBlank(correlationId) ? correlationId : UUID.randomUUID().toString();
    this.sparkConfigs = MapUtils.isNotEmpty(sparkConfigs) ? sparkConfigs : Collections.emptyMap();
  }

  public abstract Map<String, String> jobVMOptions();
}
