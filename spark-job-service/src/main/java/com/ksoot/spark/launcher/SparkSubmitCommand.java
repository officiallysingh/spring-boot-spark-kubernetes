package com.ksoot.spark.launcher;

import static com.ksoot.spark.util.Constants.*;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;

public class SparkSubmitCommand {

  public static SparkConfBuilder mainClass(String mainClass) {
    return new SparkSubmitCommandBuilder(mainClass);
  }

  public interface SparkConfBuilder {
    JobArgsBuilder sparkConfigurations(final Properties sparkConfigurations);
  }

  public interface JobArgsBuilder extends EnvVarsBuilder {
    EnvVarsBuilder jobArgs(final Map<String, String> args);
  }

  public interface EnvVarsBuilder {
    JarFileBuilder environmentVariables(final Map<String, Object> envVars);
  }

  public interface JarFileBuilder {
    VerboseBuilder jarFile(final String jarFile);
  }

  public interface VerboseBuilder extends Builder<String> {
    Builder<String> verbose(final boolean verbose);
  }

  public static class SparkSubmitCommandBuilder
      implements JobArgsBuilder, SparkConfBuilder, JarFileBuilder, VerboseBuilder {

    private final String mainClass;

    private Properties sparkConfigurations;

    private boolean verbose = true;

    private String jobArgs;

    private String envVars;

    private String jarFile;

    SparkSubmitCommandBuilder(final String mainClass) {
      this.mainClass = mainClass;
    }

    @Override
    public JobArgsBuilder sparkConfigurations(final Properties sparkConfigurations) {
      this.sparkConfigurations = sparkConfigurations;
      return this;
    }

    @Override
    public EnvVarsBuilder jobArgs(final Map<String, String> args) {
      this.jobArgs =
          args.entrySet().stream()
              .map(
                  entry -> VM_OPTION_PREFIX + entry.getKey().trim() + "=" + entry.getValue().trim())
              .collect(Collectors.joining(" "))
              .trim();
      return this;
    }

    @Override
    public JarFileBuilder environmentVariables(final Map<String, Object> envVars) {
      this.envVars =
          envVars.entrySet().stream()
              .map(
                  entry ->
                      VM_OPTION_PREFIX
                          + entry.getKey().trim()
                          + "="
                          + entry.getValue().toString().trim())
              .collect(Collectors.joining(" "))
              .trim();
      return this;
    }

    @Override
    public VerboseBuilder jarFile(final String jarFile) {
      this.jarFile = jarFile;
      return this;
    }

    @Override
    public Builder<String> verbose(final boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    @Override
    public String build() {
      String sparkDriverExtraJavaOptions =
          this.sparkConfigurations.getOrDefault(SPARK_DRIVER_EXTRA_JAVA_OPTIONS, "").toString();
      if (StringUtils.isNotBlank(this.jobArgs)) {
        sparkDriverExtraJavaOptions = (sparkDriverExtraJavaOptions + " " + this.jobArgs).trim();
      }
      if (StringUtils.isNotBlank(this.envVars)) {
        sparkDriverExtraJavaOptions = (sparkDriverExtraJavaOptions + " " + this.envVars).trim();
      }
      if (StringUtils.isNotBlank(sparkDriverExtraJavaOptions)) {
        this.sparkConfigurations.put(
            SPARK_DRIVER_EXTRA_JAVA_OPTIONS,
            StringUtils.wrap(sparkDriverExtraJavaOptions.trim(), '"'));
      }

      String sparkConf =
          sparkConfigurations.entrySet().stream()
              .map(
                  entry ->
                      CONF_PREFIX
                          + entry.getKey().toString().trim()
                          + "="
                          + entry.getValue().toString().trim())
              .collect(Collectors.joining(" "))
              .trim();

      String sparkSubmitCommand =
          "./bin/spark-submit"
              + SPACE
              + (this.verbose ? "--verbose" : "")
              + " --class "
              + this.mainClass
              + SPACE
              + sparkConf
              + SPACE
              + this.jarFile;
      return sparkSubmitCommand;
    }
  }
}
