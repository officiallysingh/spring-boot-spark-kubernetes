package com.ksoot.spark.dto;

import com.ksoot.spark.util.datetime.DurationRepresentation;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.task.repository.TaskExecution;

@Getter
@RequiredArgsConstructor
public class JobExecution {

  private final String jobName;
  private final long executionId;
  private final String correlationId;
  private final String status;
  private final LocalDateTime startTime;
  private final LocalDateTime endTime;
  private final String duration;
  private final String exitMessage;
  private final String errorMessage;
  private final List<String> arguments;

  public static JobExecution of(final TaskExecution taskExecution) {
    final DurationRepresentation duration =
        Objects.nonNull(taskExecution.getEndTime())
            ? DurationRepresentation.of(
                Duration.between(taskExecution.getStartTime(), taskExecution.getEndTime()))
            : null;
    final String status =
        Objects.isNull(taskExecution.getEndTime())
            ? "RUNNING"
            : taskExecution.getExitCode() == 0 ? "SUCCESSFUL" : "FAILED";
    return new JobExecution(
        taskExecution.getTaskName(),
        taskExecution.getExecutionId(),
        taskExecution.getExternalExecutionId(),
        status,
        taskExecution.getStartTime(),
        taskExecution.getEndTime(),
        Objects.nonNull(duration) ? duration.toString() : null,
        taskExecution.getExitMessage(),
        taskExecution.getErrorMessage(),
        taskExecution.getArguments());
  }
}
