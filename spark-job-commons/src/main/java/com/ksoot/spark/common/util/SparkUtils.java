package com.ksoot.spark.common.util;

import static com.ksoot.spark.common.util.JobConstants.BACKTICK;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.springframework.util.Assert;

@Slf4j
@UtilityClass
public class SparkUtils {

  private static final String REGEX_CONTAINS_SLASH = "(/[^\\s/]+(/[\\w]+)*)";

  public static Column toSparkColumn(final String columnName) {
    Assert.hasText(columnName, "columnName is required");
    return functions.col(columnName);
  }

  public static Column[] toSparkColumns(final Collection<String> columnNames) {
    Assert.notEmpty(columnNames, "columnNames is required");
    return columnNames.stream().map(functions::col).toArray(Column[]::new);
  }

  public static Column[] toSparkColumns(final String... columnNames) {
    Assert.notEmpty(columnNames, "columnNames is required");
    return Arrays.stream(columnNames).map(functions::col).toArray(Column[]::new);
  }

  public static boolean containColumns(final Dataset<Row> dataset, final List<String> columnNames) {
    Assert.notEmpty(columnNames, "columnNames required");
    final String[] datasetColumns = dataset.columns();
    return columnNames.stream().allMatch(col -> ArrayUtils.contains(datasetColumns, col));
  }

  public static boolean containColumns(final Dataset<Row> dataset, final String... columnNames) {
    Assert.notEmpty(columnNames, "columnNames required");
    final String[] datasetColumns = dataset.columns();
    return Arrays.stream(columnNames).allMatch(col -> ArrayUtils.contains(datasetColumns, col));
  }

  public static boolean containsColumn(final Dataset<Row> dataset, final String columnName) {
    return Arrays.stream(dataset.columns()).anyMatch(col -> col.equals(columnName));
  }

  public static boolean doesNotContainColumn(final Dataset<Row> dataset, final String columnName) {
    return Arrays.stream(dataset.columns()).noneMatch(col -> col.equals(columnName));
  }

  public static String backtickWrapColumnNamesIfContainsSlash(final String expression) {
    Assert.hasText(expression, "expression is required");
    Pattern pattern = Pattern.compile(REGEX_CONTAINS_SLASH);
    Matcher matcher = pattern.matcher(expression);
    StringBuilder newExpression = new StringBuilder();
    while (matcher.find()) {
      String columnName = matcher.group(1);
      matcher.appendReplacement(newExpression, BACKTICK + columnName + BACKTICK);
    }
    matcher.appendTail(newExpression);
    return newExpression.toString();
  }

  public static void logDataset(final String datasetName, final Dataset<Row> dataset) {
    logDataset(datasetName, dataset, 8);
  }

  public static void logDataset(
      final String datasetName, final Dataset<Row> dataset, final int numRows) {
    if (Objects.nonNull(dataset)) {
      log.info("----------- Dataset: {} -----------", datasetName);
      final String schema = dataset.schema().treeString();
      log.info("\n" + schema);
      dataset.show(numRows, false);
      log.info("..................................................");
    } else {
      log.info("----------- Dataset: {} is null -----------", datasetName);
    }
  }
}
