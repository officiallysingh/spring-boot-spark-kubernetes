package com.ksoot.spark.api;

import com.ksoot.spark.dto.JobLaunchRequest;
import com.ksoot.spark.launcher.SparkJobLauncher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/spark-jobs")
@Tag(name = "Spark Job Launcher", description = "APIs")
@Slf4j
@RequiredArgsConstructor
class SparkJobController {

  private final SparkJobLauncher sparkJobLauncher;

  @Operation(operationId = "start-spark-job", summary = "Start Spark Job")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "202",
            description =
                "Spark Job start request accepted successfully for asynchronous execution"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @PostMapping("/start")
  ResponseEntity<String> startSparkJob(@RequestBody @Valid JobLaunchRequest jobLaunchRequest) {
    log.info(
        "Start Spark Job: {} request received with correlation id: {}",
        jobLaunchRequest.getJobName(),
        jobLaunchRequest.getCorrelationId());

    this.sparkJobLauncher.startJob(jobLaunchRequest);

    return ResponseEntity.accepted()
        .body(
            "Spark Job: '"
                + jobLaunchRequest.getJobName()
                + "' submit request accepted for asynchronous execution. Correlation Id: "
                + jobLaunchRequest.getCorrelationId()
                + ". For real status of Job look into application logs or Driver POD logs if deploying on Kubernetes");
  }

  @Operation(operationId = "stop-spark-job", summary = "Stop Spark Job")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "202",
            description =
                "Spark Job stop request accepted successfully for asynchronous execution"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @PostMapping("/stop/{correlationId}")
  ResponseEntity<String> stopSparkJob(
      @Parameter(
              description = "Job Correlation Id",
              required = true,
              example = "71643ba2-1177-4e10-a43b-a21177de1022")
          @PathVariable(name = "correlationId")
          final String correlationId) {
    log.info("Stop Spark Job request received for Job with Correlation Id: {}", correlationId);

    this.sparkJobLauncher.stopJob(correlationId);

    return ResponseEntity.accepted()
        .body(
            "Spark Job stop request for Correlation Id: "
                + correlationId
                + "  accepted for asynchronous execution.");
  }
}
