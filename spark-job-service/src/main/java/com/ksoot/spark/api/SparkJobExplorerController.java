package com.ksoot.spark.api;

import com.ksoot.problem.core.Problems;
import com.ksoot.spark.dto.JobExecution;
import com.ksoot.spark.util.pagination.PaginatedResource;
import com.ksoot.spark.util.pagination.PaginatedResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Spark Job Executions", description = "APIs")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/spark-jobs/executions")
@ConditionalOnProperty(prefix = "spark-submit", name = "persist-jobs", havingValue = "true")
class SparkJobExplorerController {

  private final Function<List<TaskExecution>, List<JobExecution>> JOB_EXECUTION_PAGE_TRANSFORMER =
      taskExecutions -> taskExecutions.stream().map(JobExecution::of).toList();

  private final TaskExplorer taskExplorer;

  @Operation(operationId = "list-job-executions", summary = "Gets a page of Job executions")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job executions page returned successfully. Returns an empty page if no Job executions found"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping
  //  @GetMapping("/executions")
  PaginatedResource<JobExecution> listJobExecutions(
      @ParameterObject @PageableDefault final Pageable pageRequest) {
    Page<TaskExecution> jobExecutionsPage = this.taskExplorer.findAll(pageRequest);
    return PaginatedResourceAssembler.assemble(jobExecutionsPage, JOB_EXECUTION_PAGE_TRANSFORMER);
  }

  @Operation(
      operationId = "list-job-executions-by-job-name",
      summary = "Gets a page of Job executions by Job name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job executions page returned successfully. Returns an empty page if no Job executions found"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
  PaginatedResource<JobExecution> listJobExecutionsByJobName(
      @Parameter(description = "Job name", required = true, example = "daily-sales-report-job")
          @PathVariable(name = "jobName")
          final String jobName,
      @ParameterObject @PageableDefault final Pageable pageRequest) {
    final Page<TaskExecution> jobExecutionsPage =
        this.taskExplorer.findTaskExecutionsByName(jobName, pageRequest);
    return PaginatedResourceAssembler.assemble(jobExecutionsPage, JOB_EXECUTION_PAGE_TRANSFORMER);
  }

  @Operation(
      operationId = "list-running-job-executions-by-job-name",
      summary = "Gets a page of Running Job executions by Job name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job executions page returned successfully. Returns an empty page if no Job executions found"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/{jobName}/running", produces = MediaType.APPLICATION_JSON_VALUE)
  PaginatedResource<JobExecution> listRunningJobExecutionsByJobName(
      @Parameter(description = "Job name", required = true, example = "daily-sales-report-job")
          @PathVariable(name = "jobName")
          final String jobName,
      @ParameterObject @PageableDefault final Pageable pageRequest) {
    final Page<TaskExecution> jobExecutionsPage =
        this.taskExplorer.findRunningTaskExecutions(jobName, pageRequest);
    return PaginatedResourceAssembler.assemble(jobExecutionsPage, JOB_EXECUTION_PAGE_TRANSFORMER);
  }

  @Operation(
      operationId = "get-latest-job-execution-by-job-name",
      summary = "Gets a Latest Job execution by Job name")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Job executions returned successfully."),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/{jobName}/latest", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<JobExecution> getLatestJobExecutionByJobName(
      @Parameter(description = "Job name", required = true, example = "daily-sales-report-job")
          @PathVariable(name = "jobName")
          final String jobName) {
    final TaskExecution taskExecution =
        this.taskExplorer.getLatestTaskExecutionForTaskName(jobName);
    return Optional.ofNullable(taskExecution)
        .map(taskExec -> ResponseEntity.ok(JobExecution.of(taskExec)))
        .orElseThrow(Problems::notFound);
  }

  @Operation(
      operationId = "list-job-executions-by-correlation-id",
      summary = "Gets a page of Job executions by Job Correlation Id")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job executions page returned successfully. Returns an empty page if no Job executions found"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(
      path = "/by-correlation-id/{correlationId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  PaginatedResource<JobExecution> listJobExecutionsByCorrelationId(
      @Parameter(
              description = "Job Correlation Id",
              required = true,
              example = "71643ba2-1177-4e10-a43b-a21177de1022")
          @PathVariable(name = "correlationId")
          final String correlationId,
      @ParameterObject @PageableDefault final Pageable pageRequest) {
    final Page<TaskExecution> jobExecutionsPage =
        this.taskExplorer.findTaskExecutionsByExecutionId(correlationId, pageRequest);
    return PaginatedResourceAssembler.assemble(jobExecutionsPage, JOB_EXECUTION_PAGE_TRANSFORMER);
  }

  @Operation(operationId = "get-job-names", summary = "Gets List of Job names")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job Names returned successfully. Returns an empty List if no Jobs found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/job-names", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<String>> getJobNames() {
    final List<String> jobNames = this.taskExplorer.getTaskNames();
    return ResponseEntity.ok(jobNames);
  }

  @Operation(operationId = "get-job-executions-count", summary = "Gets Job executions Count")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job executions Count returned successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Long> getJobExecutionsCount() {
    final long executionCount = this.taskExplorer.getTaskExecutionCount();
    return ResponseEntity.ok(executionCount);
  }

  @Operation(
      operationId = "get-running-job-executions-count",
      summary = "Gets Running Job executions Count")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Running Job executions Count returned successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/count-running", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Long> getRunningJobExecutionsCount() {
    final long runningExecutionCount = this.taskExplorer.getRunningTaskExecutionCount();
    return ResponseEntity.ok(runningExecutionCount);
  }

  @Operation(
      operationId = "list-latest-job-execution-by-job-names",
      summary = "Gets List of Latest Job executions by Job names")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Job executions returned successfully. Returns an empty page if no Job executions found"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<JobExecution>> listLatestJobExecutionByJobNames(
      @Parameter(description = "Job Names") @RequestParam @NotEmpty final List<String> jobNames) {
    final List<TaskExecution> taskExecutions =
        this.taskExplorer.getLatestTaskExecutionsByTaskNames(jobNames.toArray(String[]::new));
    return ResponseEntity.ok(taskExecutions.stream().map(JobExecution::of).toList());
  }

  @Operation(
      operationId = "get-job-executions-count-by-job-name",
      summary = "Gets Job executions Count by Job Name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job executions Count returned successfully."),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(path = "/count/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Long> getJobExecutionsCountByJobName(
      @Parameter(description = "Job name", required = true, example = "daily-sales-report-job")
          @PathVariable(name = "jobName")
          final String jobName) {
    final long executionCount = this.taskExplorer.getTaskExecutionCountByTaskName(jobName);
    return ResponseEntity.ok(executionCount);
  }

  @Operation(
      operationId = "get-job-executions-count-by-correlation-id",
      summary = "Gets Job executions Count by Correlation Id")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job executions Count returned successfully."),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(
      path = "/count-by-correlation-id/{correlationId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Long> getJobExecutionsCountByCorrelationId(
      @Parameter(
              description = "Job Correlation Id",
              required = true,
              example = "71643ba2-1177-4e10-a43b-a21177de1022")
          @PathVariable(name = "correlationId")
          final String correlationId) {
    final long executionCount =
        this.taskExplorer.getTaskExecutionCountByExternalExecutionId(correlationId);
    return ResponseEntity.ok(executionCount);
  }
}
