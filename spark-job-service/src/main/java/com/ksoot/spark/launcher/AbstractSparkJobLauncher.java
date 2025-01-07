package com.ksoot.spark.launcher;

import static com.ksoot.spark.util.Constants.DEPLOY_MODE;
import static com.ksoot.spark.util.Constants.DEPLOY_MODE_CLIENT;

import com.ksoot.spark.conf.SparkJobProperties;
import com.ksoot.spark.conf.SparkLauncherProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public abstract class AbstractSparkJobLauncher implements SparkJobLauncher {

  protected final Properties sparkProperties;

  protected final SparkLauncherProperties sparkLauncherProperties;

  protected AbstractSparkJobLauncher(
      final Properties sparkProperties, final SparkLauncherProperties sparkLauncherProperties) {
    this.sparkProperties = sparkProperties;
    this.sparkLauncherProperties = sparkLauncherProperties;
  }

  protected Properties sparkConfigurations(
      final SparkJobProperties sparkJobProperties,
      final Map<String, Object> sparkRuntimeJobSpecificProperties) {
    final Properties sparkJobSpecificProperties = sparkJobProperties.getSparkConfig();
    final Properties confProperties = new Properties();
    // Overriding properties with low precedence by that with high precedence.
    confProperties.putAll(this.sparkProperties);
    confProperties.putAll(sparkJobSpecificProperties);
    confProperties.putAll(sparkRuntimeJobSpecificProperties);

    if (!confProperties.containsKey(DEPLOY_MODE)) {
      log.info(DEPLOY_MODE + " not specified, falling back to: " + DEPLOY_MODE_CLIENT);
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

  protected Map<String, Object> environmentVariables(final SparkJobProperties sparkJobProperties) {
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
}
