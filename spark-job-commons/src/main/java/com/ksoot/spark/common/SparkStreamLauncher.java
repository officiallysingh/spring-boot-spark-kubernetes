package com.ksoot.spark.common;

import com.ksoot.spark.common.util.StreamRetryableException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Log4j2
@RequiredArgsConstructor
public class SparkStreamLauncher {

  private final SparkExecutionManager sparkExecutionManager;

  private final TaskExecutor taskExecutor;

  /**
   * Start a stream with a fault-tolerant retryable mechanism. If the stream fails, it will retry
   * after 5 seconds.
   *
   * @param dataStreamWriter The stream to start.
   */
  public void startStream(final DataStreamWriter<?> dataStreamWriter) {
    this.taskExecutor.execute(() -> this.startAndAwaitRetryableStream(dataStreamWriter));
  }

  /**
   * Start a stream. If the stream fails, it will retry after 5 seconds.
   *
   * @param dataStreamWriter The stream to start.
   * @param faultTolerant If true, the stream will be fault-tolerant and will retry indefinitely
   *     after 5 seconds if it fails, otherwise the stream will fail one error.
   */
  @SneakyThrows
  public void startStream(final DataStreamWriter<?> dataStreamWriter, final boolean faultTolerant) {
    if (faultTolerant) {
      this.startStream(dataStreamWriter);
    } else {
      dataStreamWriter.start().awaitTermination();
      this.taskExecutor.execute(
          () -> {
            try {
              dataStreamWriter.start().awaitTermination();
            } catch (final StreamingQueryException | TimeoutException e) {
              log.error("Exception in Spark stream: {}.", e.getMessage());
            }
          });
    }
  }

  @Retryable(
      retryFor = {StreamRetryableException.class},
      maxAttempts = Integer.MAX_VALUE,
      backoff = @Backoff(delay = 5000)) // Delay of 5 seconds, with unlimited retry attempts
  private void startAndAwaitRetryableStream(final DataStreamWriter<?> dataStreamWriter) {
    try {
      final StreamingQuery streamingQuery = dataStreamWriter.start();
      this.sparkExecutionManager.addStreamingQuery(streamingQuery);
      streamingQuery.awaitTermination();
    } catch (final TimeoutException | StreamingQueryException e) {
      log.error(
          "Exception in Spark stream: {}. Will retry to recover from error after 5 seconds",
          e.getMessage());
      throw new StreamRetryableException("Exception in spark streaming", e);
    }
  }
}
