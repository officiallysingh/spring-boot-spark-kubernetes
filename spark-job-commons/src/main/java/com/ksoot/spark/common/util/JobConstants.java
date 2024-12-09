package com.ksoot.spark.common.util;

import java.time.format.DateTimeFormatter;

public class JobConstants {

  public static final String JOB_NAME_SPARK_STATEMENT_JOB = "spark-statement-job";
  public static final String JOB_NAME_SPARK_WORD_COUNT_JOB = "spark-word-count-job";

  public static final String ID = "id";
  public static final String VALUE = "value";

  public static final char BACKTICK = '`';
  public static final char QUOTE = '"';
  public static final String REGEX_STRING_BEFORE_SLASH = ".*/";
  public static final String REGEX_SPACE = "\\s+";
  public static final String SLASH = "/";
  public static final String SPACE = " ";
  public static final String BLANK = "";
  public static final String COMMA = ",";
  public static final String COLON = ": ";
  public static final String EQUAL_TO = " = ";
  public static final String OR = " OR ";
  public static final String AND = " AND ";
  public static final String IN = " IN ";
  public static final String UNDERSCORE = "_";
  public static final String DOT = ".";

  public static DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
}
