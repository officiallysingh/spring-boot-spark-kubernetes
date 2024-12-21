package com.ksoot.spark.loganalysis;

import static org.apache.commons.lang3.StringUtils.capitalize;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.datafaker.Faker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Log4j2
@RequiredArgsConstructor
public class LogsGenerator {

  private static final Faker faker =
      new Faker(new Locale.Builder().setLanguage("en").setRegion("US").build());

  private static final String[] LEVELS = {
    "INFO", "DEBUG", "WARN", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR"
  };
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX");

  private static final String LOG_TEMPLATE =
      "${timestamp} ${level} ${pid} --- [${application}] [  ${thread}] ${class}     : ${message}";

  private final ConnectorProperties connectorProperties;

  private final KafkaTemplate<String, String> kafkaTemplate;

  // Write a random log line to kafka topic from where Spark stream can read the messages and
  // process
  @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
  void generateLogs() {
    final String timestamp = OffsetDateTime.now().format(formatter);
    final String level = faker.options().option(LEVELS);
    final String pid = faker.options().option("86228", "23567", "34556", "34122", "56777");
    final String application =
        faker.options().option("AppOne", "AppTwo", "AppThree", "AppFour", "AppFive");
    final String thread =
        faker.options().option("restartedMain", "shutdown-hook-0", "connectionPool-1");
    final String packageName =
        faker.app().name().toLowerCase().replaceAll("\\s+", "")
            + "."
            + faker.hacker().adjective().toLowerCase()
            + "."
            + faker.hacker().noun().toLowerCase();
    final String className =
        capitalize(faker.hacker().adjective()) + capitalize(faker.hacker().noun());
    final String message =
        faker.hacker().verb()
            + " at "
            + faker.file().fileName()
            + ". Cause "
            + faker.hacker().noun();
    String logLine =
        substitute(
            LOG_TEMPLATE,
            "timestamp",
            timestamp,
            "level",
            level,
            "pid",
            pid,
            "application",
            application,
            "class",
            packageName + className,
            "thread",
            thread,
            "message",
            message);
    System.out.println(
        "Writing log line: '"
            + logLine
            + "' to kafka topic: '"
            + this.connectorProperties.getKafkaOptions().getTopic()
            + "'");
    //    log.debug("Writing log line: {} to kafka topic: {}", logLine,
    // this.connectorProperties.getKafkaOptions().getTopic());
    this.kafkaTemplate.send(this.connectorProperties.getKafkaOptions().getTopic(), logLine);
  }

  private static String substitute(final String template, final String... vars) {
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
    return substitute(template, params);
  }

  private static String substitute(final String template, final Map<String, String> vars) {
    Assert.hasText(template, "template is required");
    Assert.notEmpty(vars, "vars is required");
    return StringSubstitutor.replace(template, vars);
  }
}
