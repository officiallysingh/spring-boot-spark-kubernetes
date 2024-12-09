package com.ksoot.spark.api;

import com.ksoot.spark.SparkSubmitter;
import com.ksoot.spark.dto.JobSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/spark-jobs")
@Tag(name = "Spark Job Submit", description = "APIs")
@Slf4j
@RequiredArgsConstructor
class SparkJobController {

  private final SparkSubmitter sparkSubmitter;

  @Operation(operationId = "submit-spark-job", summary = "Submit Spark Job")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "202",
            description =
                "Spark Job submit request accepted successfully for asynchronous execution"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @PostMapping("/submit")
  ResponseEntity<String> submitSparkJob(@RequestBody @Valid JobSubmitRequest jobSubmitRequest)
      throws IOException, InterruptedException {
    log.info("Submitting Spark Job: {}", jobSubmitRequest.getJobName());

    this.sparkSubmitter.submit(jobSubmitRequest);

    return ResponseEntity.accepted()
        .body(
            "Spark Job: '"
                + jobSubmitRequest.getJobName()
                + "' submit request accepted for asynchronous execution. Correlation Id: "
                + jobSubmitRequest.getCorrelationId());
  }
}
