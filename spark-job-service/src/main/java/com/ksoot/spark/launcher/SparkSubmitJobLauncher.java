package com.ksoot.spark.launcher;

import static com.ksoot.spark.util.Constants.*;

import com.ksoot.problem.core.Problems;
import com.ksoot.spark.conf.SparkJobProperties;
import com.ksoot.spark.conf.SparkLauncherProperties;
import com.ksoot.spark.dto.JobLaunchRequest;
import io.micrometer.tracing.Tracer;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class SparkSubmitJobLauncher extends AbstractSparkJobLauncher {

  private final KafkaTemplate<String, String> kafkaTemplate;

  private final ExecutorService executor;

  private final String jobStopTopic;

  private final Tracer tracer;

  public SparkSubmitJobLauncher(
      @Qualifier("sparkProperties") final Properties sparkProperties,
      final SparkLauncherProperties sparkLauncherProperties,
      final KafkaTemplate<String, String> kafkaTemplate,
      @Value("${spark-launcher.job-stop-topic}") final String jobStopTopic,
      final Tracer tracer) {
    super(sparkProperties, sparkLauncherProperties);
    this.kafkaTemplate = kafkaTemplate;
    this.jobStopTopic = jobStopTopic;
    this.tracer = tracer;
    this.executor = Executors.newCachedThreadPool();
  }

  public void startJob(final JobLaunchRequest jobLaunchRequest) {
    log.info("============================================================");
    //    headers.set("X-B3-TraceId", "abc123");
    //    headers.set("X-B3-SpanId", "spanA");
    String traceId = this.tracer.currentTraceContext().context().traceId();
    String spanId = this.tracer.currentTraceContext().context().spanId();

    final SparkJobProperties sparkJobProperties =
        Optional.ofNullable(
                this.sparkLauncherProperties.getJobs().get(jobLaunchRequest.getJobName()))
            .orElseThrow(
                () ->
                    Problems.newInstance("invalid.job.name")
                        .defaultDetail("Invalid Job name: {0}. Allowed values: {1}")
                        .detailArgs(
                            jobLaunchRequest.getJobName(),
                            String.join(", ", this.sparkLauncherProperties.getJobs().keySet()))
                        .throwAble(HttpStatus.BAD_REQUEST));
    final Properties sparkConfigurations =
        this.sparkConfigurations(sparkJobProperties, jobLaunchRequest.getSparkConfigs());

    final Map<String, Object> envVars = this.environmentVariables(sparkJobProperties);

    final Map<String, String> jobArgs = new LinkedHashMap<>(jobLaunchRequest.jobArgs());
    jobArgs.put(CORRELATION_ID, jobLaunchRequest.getCorrelationId());
    jobArgs.put(PERSIST_JOB, String.valueOf(this.sparkLauncherProperties.isPersistJobs()));

    final String sparkSubmitCommand =
        SparkSubmitCommand.jobName(jobLaunchRequest.getJobName())
            .mainClass(sparkJobProperties.getMainClassName())
            .sparkConfigurations(sparkConfigurations)
            .jobArgs(jobArgs)
            .environmentVariables(envVars)
            .jarFile(sparkJobProperties.getJarFile())
            .build();

    log.info("spark-submit command: {}", sparkSubmitCommand);

    try {
      CompletableFuture<Process> process = this.sparkSubmit(sparkSubmitCommand);

      process.thenAccept(
          p ->
              log.info(
                  "spark-submit completed with exitValue: {}, Command status: {} for job name: {} and correlation id: {}. "
                      + "Command status does not represent actual Job status, look into application logs or Driver POD logs for details",
                  p.exitValue(),
                  (p.exitValue() == 0 ? "SUCCESS" : "FAILURE"),
                  jobLaunchRequest.getJobName(),
                  jobLaunchRequest.getCorrelationId()));
    } catch (final IOException | InterruptedException e) {
      throw Problems.newInstance("spark.submit.error")
          .defaultDetail(
              "Something went wrong while executing spark-submit command. Please look into logs for details.")
          .cause(e)
          .throwAble(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    log.info("============================================================");
  }

  private CompletableFuture<Process> sparkSubmit(final String sparkSubmitCommand)
      throws IOException, InterruptedException {

    final File directory = new File(this.sparkLauncherProperties.getSparkHome());

    final String sparkSubmitScriptPath = this.getSparkSubmitScriptPath();

    final ProcessBuilder processBuilder =
        new ProcessBuilder(sparkSubmitScriptPath).directory(directory);

    final Map<String, String> environment = processBuilder.environment();
    environment.put("JOB_SUBMIT_COMMAND", sparkSubmitCommand);

    final Process process;
    // Start the process
    if (this.sparkLauncherProperties.isCaptureJobsLogs()) {
      process = processBuilder.inheritIO().start();
      final StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), log::info);
      final StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), log::error);
      executor.submit(outputGobbler);
      executor.submit(errorGobbler);
    } else {
      process = processBuilder.start();
    }

    return process.onExit();
  }

  private String getSparkSubmitScriptPath() {
    final String osName = System.getProperty("os.name").toLowerCase();
    String sparkSubmitScriptFile = SPARK_SUBMIT_SCRIPT + (osName.contains("win") ? ".bat" : ".sh");
    String sparkSubmitScriptPath;
    try {
      // A Heck to get spark-job-submit.sh path at project root while running in local
      final Resource resource =
          new ClassPathResource("META-INF/additional-spring-configuration-metadata.json");
      final Path currentPath = resource.getFile().toPath();
      sparkSubmitScriptPath =
          currentPath.getParent().getParent().getParent().getParent().toString()
              + "/cmd/"
              + sparkSubmitScriptFile;
    } catch (final FileNotFoundException e) {
      sparkSubmitScriptPath = "./bin/" + sparkSubmitScriptFile;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    log.info("spark-job-submit file path: {}", sparkSubmitScriptPath);
    return sparkSubmitScriptPath;
  }

  static class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private final Consumer<String> consumer;

    StreamGobbler(final InputStream inputStream, final Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }
  }

  @Override
  public void stopJob(final String jobCorrelationId) {
    this.kafkaTemplate.send(this.jobStopTopic, jobCorrelationId);
  }
}
