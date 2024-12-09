package com.ksoot.spark.common.config;

import com.ksoot.spark.common.error.JobErrorType;
import com.ksoot.spark.common.error.JobProblem;
import com.ksoot.spark.common.util.DurationRepresentation;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.MessageSource;

@Log4j2
@RequiredArgsConstructor
public class JobExecutionListener {

  private final MessageSource messageSource;

  @BeforeTask
  public void onJobStart(final TaskExecution taskExecution) {
    log.info(
        "Job: {} with executionId: {} and externalExecutionId: {} started at: {} with arguments: {}",
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        taskExecution.getStartTime(),
        taskExecution.getArguments());
  }

  @AfterTask
  public void onJobSuccess(final TaskExecution taskExecution) {
    if (taskExecution.getExitCode() == 0) {
      DurationRepresentation duration =
          DurationRepresentation.of(
              Duration.between(taskExecution.getStartTime(), taskExecution.getEndTime()));
      log.info(
          "Job: {} with executionId: {} and externalExecutionId: {} completed successfully at: {} with exitCode: {} and exitMessage: {}. "
              + "Total time taken: {}",
          taskExecution.getTaskName(),
          taskExecution.getExecutionId(),
          taskExecution.getExternalExecutionId(),
          taskExecution.getEndTime(),
          taskExecution.getExitCode(),
          taskExecution.getExitMessage(),
          duration);
    }
  }

  @FailedTask
  public void onJobFailure(final TaskExecution taskExecution, final Throwable throwable) {
    DurationRepresentation duration =
        DurationRepresentation.of(
            Duration.between(taskExecution.getStartTime(), taskExecution.getEndTime()));
    log.error(
        "Task: {} with executionId: {} and externalExecutionId: {} failed at: {} with exitCode: {} and exitMessage: {}. "
            + "Total time taken: {}",
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        taskExecution.getEndTime(),
        taskExecution.getExitCode(),
        taskExecution.getExitMessage(),
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
  }

  private String getMessage(final String messageCode, final String defaultMessage) {
    return this.messageSource.getMessage(messageCode, null, defaultMessage, Locale.getDefault());
  }

  private String getMessage(
      final String messageCode, final String defaultMessage, final Object... params) {
    return this.messageSource.getMessage(messageCode, params, defaultMessage, Locale.getDefault());
  }
}
