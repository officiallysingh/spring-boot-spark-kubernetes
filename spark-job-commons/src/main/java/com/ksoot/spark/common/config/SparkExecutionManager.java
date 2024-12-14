package com.ksoot.spark.common.config;

import com.ksoot.spark.common.error.JobErrorType;
import com.ksoot.spark.common.error.JobProblem;
import com.ksoot.spark.common.util.DurationRepresentation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.Lifecycle;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.util.Assert;

@Log4j2
@RequiredArgsConstructor
public class SparkExecutionManager {

  private final SparkSession sparkSession;

  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  private final MessageSource messageSource;

  private final List<StreamingQuery> streamingQueries = new ArrayList<>();

  @Value("${ksoot.job.correlation-id}")
  private String jobCorrelationId;

  private int exitCode = -1;

  private String exitMessage = null;

  public void addStreamingQuery(final StreamingQuery streamingQuery) {
    Assert.notNull(streamingQuery, "streamingQuery must not be null");
    this.streamingQueries.add(streamingQuery);
  }

  public void addStreamingQueries(final List<StreamingQuery> streamingQueries) {
    Assert.notEmpty(streamingQueries, "streamingQueries must not be empty");
    Assert.noNullElements(streamingQueries, "streamingQueries must not contain null elements");
    this.streamingQueries.addAll(streamingQueries);
  }

  @KafkaListener(
      topics = "job-stop-requests",
      groupId = "#{T(java.util.UUID).randomUUID().toString()}")
  void onJobStopRequest(final String correlationId) {
    if (!this.jobCorrelationId.equals(correlationId)) {
      return;
    }
    log.info("Job stop request received for correlationId: {}", correlationId);
    try {
      if (CollectionUtils.isNotEmpty(this.streamingQueries)) {
        for (StreamingQuery streamingQuery : this.streamingQueries) {
          try {
            //            if(streamingQuery.isActive())
            streamingQuery.stop();
          } catch (final Exception e) {
            // Ignored on purpose
          }
        }
      }
    } finally {
      this.exitCode = 2;
      this.exitMessage = "Termination requested";
      final SparkContext sparkContext = this.sparkSession.sparkContext();
      if (!sparkContext.isStopped()) {
        sparkContext.cancelAllJobs();
        //        sparkContext.stop(1);
        sparkContext.stop();
      } else {
        log.info("SparkContext is already stopped for Job with correlationId: {}", correlationId);
      }
    }
  }

  @BeforeTask
  public void onJobStart(final TaskExecution taskExecution) {
    log.info(
        "Job: {} with executionId: {} and correlationId: {} started at: {} with arguments: {}",
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        taskExecution.getStartTime(),
        taskExecution.getArguments());
  }

  @AfterTask
  public void onJobCompletion(final TaskExecution taskExecution) {
    DurationRepresentation duration =
        DurationRepresentation.of(
            Duration.between(taskExecution.getStartTime(), taskExecution.getEndTime()));
    if (this.exitCode != -1) {
      //      taskExecution.setExitCode(this.exitCode); // You can not change exit code, it is
      // derived in task lifecycle listener
      taskExecution.setExitMessage(this.exitMessage);
    } else if (StringUtils.isBlank(taskExecution.getExitMessage())) {
      taskExecution.setExitMessage("Completed Successfully");
    }
    log.info(
        "Job: {} with executionId: {} and correlationId: {} {} at: {} with exitCode: {} and exitMessage: {}. "
            + "Total time taken: {}",
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        this.exitCode == -1 ? "completed successfully" : "failed",
        taskExecution.getEndTime(),
        taskExecution.getExitCode(),
        taskExecution.getExitMessage(),
        duration);

    this.stopKafkaListeners();
  }

  @FailedTask
  public void onJobFailure(final TaskExecution taskExecution, final Throwable throwable) {
    DurationRepresentation duration =
        DurationRepresentation.of(
            Duration.between(taskExecution.getStartTime(), taskExecution.getEndTime()));
    if (StringUtils.isBlank(taskExecution.getExitMessage())) {
      taskExecution.setExitMessage("Failed");
    }
    log.error(
        "Task: {} with executionId: {} and correlationId: {} failed at: {} with exitCode: {} and exitMessage: {}. "
            + "Total time taken: {}",
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        taskExecution.getEndTime(),
        taskExecution.getExitCode(),
        taskExecution.getErrorMessage(),
        duration);
    JobProblem jobProblem;
    if (throwable instanceof JobProblem e) {
      jobProblem = e;
    } else {
      jobProblem = JobProblem.of(JobErrorType.unknown()).cause(throwable).build();
    }

    final String code = jobProblem.getErrorType().code();
    final String defaultTitle = jobProblem.getErrorType().title();
    final String defaultMessage = jobProblem.getErrorType().message();
    final Object[] args = jobProblem.getArgs();

    final String title = this.getMessage("title." + code, defaultTitle);
    final String message = this.getMessage("message." + code, defaultMessage, args);

    log.error("Spark Exception[ Code: {}, Title: {}, Message: {} ]", code, title, message);
    this.stopKafkaListeners();
  }

  private String getMessage(final String messageCode, final String defaultMessage) {
    return this.messageSource.getMessage(messageCode, null, defaultMessage, Locale.getDefault());
  }

  private String getMessage(
      final String messageCode, final String defaultMessage, final Object... params) {
    return this.messageSource.getMessage(messageCode, params, defaultMessage, Locale.getDefault());
  }

  private void stopKafkaListeners() {
    this.kafkaListenerEndpointRegistry.getAllListenerContainers().forEach(Lifecycle::stop);
  }
}
