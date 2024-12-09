package com.ksoot.spark;

import com.ksoot.problem.core.Problems;
import com.ksoot.spark.conf.SparkJobProperties;
import com.ksoot.spark.conf.SparkSubmitProperties;
import com.ksoot.spark.dto.JobSubmitRequest;
import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
public class SparkSubmitter {

  public static final String CORRELATION_ID = "spring.cloud.task.external-execution-id";

  private static final String SPARK_SUBMIT_SCRIPT = "spark-job-submit";
  private static final String SPARK_DRIVER_EXTRA_JAVA_OPTIONS = "spark.driver.extraJavaOptions";

  private static final String JOB_NAME = "jobName";
  private static final String MAIN_CLASS_NAME = "mainClassName";
  private static final String CONF_PROPERTY = "confProperty";
  private static final String DEPLOY_MODE = "spark.submit.deployMode";
  private static final String DEPLOY_MODE_CLIENT = "client";

  //  private static final String SUBMIT_CMD_TEMPLATE =
  //      "./bin/spark-submit --verbose --name ${" + JOB_NAME + "} --class ${" + MAIN_CLASS_NAME +
  // "} ";
  private static final String SUBMIT_CMD_TEMPLATE =
      "./bin/spark-submit --verbose --class ${" + MAIN_CLASS_NAME + "} ";
  private static final String SUBMIT_CMD_CONF_TEMPLATE = "--conf ${confProperty}";

  private static final ExecutorService executor = Executors.newCachedThreadPool();
  private static final String VM_OPTION_PREFIX = "-D";

  private final Properties sparkProperties;

  private final SparkSubmitProperties sparkSubmitProperties;

  SparkSubmitter(
      @Qualifier("sparkProperties") final Properties sparkProperties,
      final SparkSubmitProperties sparkSubmitProperties) {
    this.sparkProperties = sparkProperties;
    this.sparkSubmitProperties = sparkSubmitProperties;
  }

  public void submit(final JobSubmitRequest jobSubmitRequest)
      throws InterruptedException, IOException {
    Assert.hasText(jobSubmitRequest.getJobName(), "jobName is required");

    log.info("============================================================");

    final SparkJobProperties sparkJobProperties =
        Optional.ofNullable(this.sparkSubmitProperties.getJobs().get(jobSubmitRequest.getJobName()))
            .orElseThrow(
                () ->
                    Problems.newInstance("invalid.job.name")
                        .defaultDetail("Invalid Job name: {0}. Allowed values: {1}")
                        .detailArgs(
                            jobSubmitRequest.getJobName(),
                            String.join(", ", this.sparkSubmitProperties.getJobs().keySet()))
                        .throwAble(HttpStatus.BAD_REQUEST));
    final Properties jobSparkConf =
        this.confProperties(
            this.sparkProperties,
            sparkJobProperties.getSparkConfig(),
            jobSubmitRequest.getSparkConfigs());

    final String baseCommand =
        this.substitute(
            SUBMIT_CMD_TEMPLATE,
            JOB_NAME,
            jobSubmitRequest.getJobName(),
            MAIN_CLASS_NAME,
            sparkJobProperties.getMainClassName());
    final String confCommand = this.getConfCommand(jobSparkConf, jobSubmitRequest);
    final String jobSubmitCommand =
        baseCommand + confCommand + " " + sparkJobProperties.getJarFile();
    log.info("spark-submit command: {}", jobSubmitCommand);

    final File directory = new File(this.sparkSubmitProperties.getSparkHome());

    final String sparkSubmitScriptPath = this.getSparkSubmitScriptPath();

    final ProcessBuilder processBuilder =
        new ProcessBuilder(sparkSubmitScriptPath).directory(directory);

    final Map<String, String> environment = processBuilder.environment();
    environment.put("JOB_SUBMIT_COMMAND", jobSubmitCommand);

    final Process process;
    final int exitCode;
    // Start the process
    if (this.sparkSubmitProperties.isCaptureJobsLogs()) {
      process = processBuilder.inheritIO().start();
      final StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), log::info);
      final StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), log::error);
      executor.submit(outputGobbler);
      executor.submit(errorGobbler);
    } else {
      process = processBuilder.start();
    }

    // Wait for the process to complete
    exitCode = process.waitFor();

    // Get the inputs.waitFor();
    log.info(
        "spark-submit completed with code: {}, status: {}",
        exitCode,
        (exitCode == 0 ? "SUCCESS" : "FAILURE"));
    log.info("============================================================");
  }

  private String getSparkSubmitScriptPath() throws IOException {
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
    }
    log.info("spark-job-submit file path: {}", sparkSubmitScriptPath);
    return sparkSubmitScriptPath;
  }

  private Properties confProperties(
      final Properties sparkCommonProperties,
      final Properties sparkJobSpecificProperties,
      final Map<String, Object> sparkRuntimeJobSpecificProperties) {

    Properties confProperties =
        this.mergeProperties(
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
  private Properties mergeProperties(
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

  //  // Merge common properties with job-specific properties, giving precedence to job-specific
  //  // properties
  //  private Properties mergeProperties(
  //      final Properties sparkCommonProperties, final Properties sparkJobSpecificProperties, final
  // Map<String, Object> sparkRuntimeJobSpecificProperties) {
  //    final Properties confProperties = new Properties();
  //    confProperties.putAll(sparkCommonProperties);
  //    confProperties.putAll(sparkJobSpecificProperties);
  //    confProperties.putAll(sparkRuntimeJobSpecificProperties);
  //    // Override/merge extraJavaOptions and add prefix -D to correctly pass them to spark submit
  //    // command.
  //    if (sparkCommonProperties.containsKey(SPARK_DRIVER_EXTRA_JAVA_OPTIONS)
  //        && sparkJobSpecificProperties.containsKey(SPARK_DRIVER_EXTRA_JAVA_OPTIONS)) {
  //      final String defaultDriverExtraJavaOptions =
  //          sparkCommonProperties.getProperty(SPARK_DRIVER_EXTRA_JAVA_OPTIONS);
  //      final String jobSpecificDriverExtraJavaOptions =
  //          sparkJobSpecificProperties.getProperty(SPARK_DRIVER_EXTRA_JAVA_OPTIONS);
  //      final Map<String, String> defaultDriverExtraJavaOptionsMap =
  //          Arrays.stream(defaultDriverExtraJavaOptions.split(" "))
  //              .map(pair -> pair.split("=", 2)) // Split each pair into key and value
  //              .collect(
  //                  Collectors.toMap(
  //                      keyValue -> keyValue[0].trim(), // Key
  //                      keyValue ->
  //                          keyValue.length > 1
  //                              ? keyValue[1].trim()
  //                              : "" // Value or empty string if no value
  //                      ));
  //      final Map<String, String> jobSpecificDriverExtraJavaOptionsMap =
  //          Arrays.stream(jobSpecificDriverExtraJavaOptions.split(" "))
  //              .map(keyValue -> keyValue.split("=", 2)) // Split each pair into key and value
  //              .collect(
  //                  Collectors.toMap(
  //                      keyValue -> keyValue[0].trim(), // Key
  //                      keyValue ->
  //                          keyValue.length > 1
  //                              ? keyValue[1].trim()
  //                              : "" // Value or empty string if no value
  //                      ));
  //      defaultDriverExtraJavaOptionsMap.putAll(jobSpecificDriverExtraJavaOptionsMap);
  //
  //      final String driverExtraJavaOptions =
  //          defaultDriverExtraJavaOptionsMap.entrySet().stream()
  //              .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
  //              .map(entry -> entry.getKey().trim() + "=" + entry.getValue().trim())
  //              .collect(Collectors.joining(" "));
  //      confProperties.put(SPARK_DRIVER_EXTRA_JAVA_OPTIONS, driverExtraJavaOptions);
  //    }
  //
  //    if (confProperties.containsKey(SPARK_DRIVER_EXTRA_JAVA_OPTIONS)) {
  //      final String driverExtraJavaOptions =
  //          Arrays.stream(confProperties.getProperty(SPARK_DRIVER_EXTRA_JAVA_OPTIONS).split(" "))
  //              .filter(
  //                  vmOption ->
  //                      vmOption.contains("=")
  //                          && !vmOption.split("=", 2)[1].isBlank()) // Filter non-blank values
  //              .map(
  //                  vmOption ->
  //                      vmOption.startsWith(VM_OPTION_PREFIX)
  //                          ? vmOption
  //                          : VM_OPTION_PREFIX + vmOption) // Add -D prefix
  //              .collect(Collectors.joining(" ")); // Join with space
  //      confProperties.put(SPARK_DRIVER_EXTRA_JAVA_OPTIONS, driverExtraJavaOptions);
  //    }
  //
  //    return confProperties;
  //  }

  private String getConfCommand(
      final Properties jobSpecificSparkConf, final JobSubmitRequest jobSubmitRequest) {
    final String runtimeJobVMOptions = this.runtimeJobVMOptions(jobSubmitRequest);
    final String deployTimeJobVMOptions =
        jobSpecificSparkConf.getProperty(SPARK_DRIVER_EXTRA_JAVA_OPTIONS);
    String sparkExtraJavaOptions =
        (StringUtils.defaultIfBlank(runtimeJobVMOptions, "")
                + " "
                + StringUtils.defaultIfBlank(deployTimeJobVMOptions, ""))
            .trim();

    final String confCommand =
        jobSpecificSparkConf.entrySet().stream()
            .filter(
                property ->
                    !property.getKey().toString().startsWith(SPARK_DRIVER_EXTRA_JAVA_OPTIONS))
            .map(property -> property.getKey() + "=" + property.getValue())
            .map(
                confProperty ->
                    this.substitute(SUBMIT_CMD_CONF_TEMPLATE, CONF_PROPERTY, confProperty))
            .collect(Collectors.joining(" "));
    if (StringUtils.isNotBlank(sparkExtraJavaOptions)) {
      sparkExtraJavaOptions =
          this.substitute(
              SUBMIT_CMD_CONF_TEMPLATE,
              CONF_PROPERTY,
              SPARK_DRIVER_EXTRA_JAVA_OPTIONS + "=" + StringUtils.wrap(sparkExtraJavaOptions, '"'));
      return confCommand.trim() + " " + sparkExtraJavaOptions;
    } else {
      return confCommand;
    }
  }

  private String runtimeJobVMOptions(final JobSubmitRequest jobSubmitRequest) {
    final String jobVMOptions =
        jobSubmitRequest.jobVMOptions().entrySet().stream()
            .map(
                property ->
                    VM_OPTION_PREFIX + property.getKey().trim() + "=" + property.getValue().trim())
            .collect(Collectors.joining(" "));
    return (jobVMOptions
            + " "
            + VM_OPTION_PREFIX
            + CORRELATION_ID
            + "="
            + jobSubmitRequest.getCorrelationId()
            + " "
            + VM_OPTION_PREFIX
            + "PERSIST_JOB"
            + "="
            + this.sparkSubmitProperties.isPersistJobs())
        .trim();
  }

  private String substitute(final String template, final String... vars) {
    Assert.hasText(template, "template is required");
    Assert.notEmpty(vars, "vars is required");
    if (vars.length % 2 != 0) {
      throw new IllegalArgumentException(
          "vars array must have an even number of elements, to create a Map.");
    }
    final Map<String, String> params =
        IntStream.range(0, vars.length / 2)
            .mapToObj(i -> ImmutablePair.of(vars[2 * i], vars[2 * i + 1]))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return this.substitute(template, params);
  }

  private String substitute(final String template, final Map<String, String> vars) {
    Assert.hasText(template, "template is required");
    Assert.notEmpty(vars, "vars is required");
    return StringSubstitutor.replace(template, vars);
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
}
