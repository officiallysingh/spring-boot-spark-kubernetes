package com.ksoot.spark.launcher;

import static com.ksoot.spark.util.Constants.*;

import com.ksoot.problem.core.Problems;
import com.ksoot.spark.conf.SparkJobProperties;
import com.ksoot.spark.conf.SparkLauncherProperties;
import com.ksoot.spark.dto.JobLaunchRequest;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SparkSubmitJobLauncher implements SparkJobLauncher {

  private static final ExecutorService executor = Executors.newCachedThreadPool();

  private final Properties sparkProperties;

  private final SparkLauncherProperties sparkLauncherProperties;

  public SparkSubmitJobLauncher(
      @Qualifier("sparkProperties") final Properties sparkProperties,
      final SparkLauncherProperties sparkLauncherProperties) {
    this.sparkProperties = sparkProperties;
    this.sparkLauncherProperties = sparkLauncherProperties;
  }

  public void startJob(final JobLaunchRequest jobLaunchRequest) {
    log.info("============================================================");

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
        this.sparkConfigurations(
            this.sparkProperties,
            sparkJobProperties.getSparkConfig(),
            jobLaunchRequest.getSparkConfigs());

    final Map<String, Object> envVars = this.mergeEnvironmentVariables(sparkJobProperties);

    final Map<String, String> jobArgs = new LinkedHashMap<>(jobLaunchRequest.jobArgs());
    jobArgs.put(CORRELATION_ID, jobLaunchRequest.getCorrelationId());
    jobArgs.put(PERSIST_JOB, String.valueOf(this.sparkLauncherProperties.isPersistJobs()));

    String sparkSubmitCommand =
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

  private Properties sparkConfigurations(
      final Properties sparkCommonProperties,
      final Properties sparkJobSpecificProperties,
      final Map<String, Object> sparkRuntimeJobSpecificProperties) {

    Properties confProperties =
        this.mergeSparkConfigurations(
            sparkCommonProperties, sparkJobSpecificProperties, sparkRuntimeJobSpecificProperties);

    if (!confProperties.containsKey(DEPLOY_MODE)) {
      log.warn(DEPLOY_MODE + " not specified, falling back to: " + DEPLOY_MODE_CLIENT);
      confProperties.putIfAbsent(DEPLOY_MODE, DEPLOY_MODE_CLIENT);
    }
    return confProperties.entrySet().stream()
        .filter(
            property -> property != null && StringUtils.isNotBlank(property.getValue().toString()))
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().toString(),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                Properties::new));
  }

  // Merge common properties with job-specific properties, giving precedence to job-specific
  // properties
  private Properties mergeSparkConfigurations(
      final Properties sparkCommonProperties,
      final Properties sparkJobSpecificProperties,
      final Map<String, Object> sparkRuntimeJobSpecificProperties) {
    final Properties confProperties = new Properties();
    // Overriding properties with low precedence by that with high precedence.
    confProperties.putAll(sparkCommonProperties);
    confProperties.putAll(sparkJobSpecificProperties);
    confProperties.putAll(sparkRuntimeJobSpecificProperties);
    return confProperties;
  }

  private Map<String, Object> mergeEnvironmentVariables(
      final SparkJobProperties sparkJobProperties) {
    Map<String, Object> mergedEnvVars = new LinkedHashMap<>();
    mergedEnvVars.putAll(this.sparkLauncherProperties.getEnv());
    mergedEnvVars.putAll(sparkJobProperties.getEnv());
    mergedEnvVars =
        mergedEnvVars.entrySet().stream()
            .filter(
                property ->
                    property != null && StringUtils.isNotBlank(property.getValue().toString()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return mergedEnvVars;
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
  public void stopJob(String jobCorrelationId) {}
}
