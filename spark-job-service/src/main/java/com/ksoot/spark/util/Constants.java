package com.ksoot.spark.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String CORRELATION_ID = "CORRELATION_ID";
  public static final String PERSIST_JOB = "PERSIST_JOB";

  public static final String SPARK_SUBMIT_SCRIPT = "spark-job-submit";
  public static final String DEPLOY_MODE = "spark.submit.deployMode";
  public static final String DEPLOY_MODE_CLIENT = "client";
  public static final String SPARK_DRIVER_EXTRA_JAVA_OPTIONS = "spark.driver.extraJavaOptions";

  public static final String SPACE = " ";
  public static final String CONF_PREFIX = "--conf ";
  //    public static final String ARG_PREFIX = "--";
  public static final String VM_OPTION_PREFIX = "-D";
}
