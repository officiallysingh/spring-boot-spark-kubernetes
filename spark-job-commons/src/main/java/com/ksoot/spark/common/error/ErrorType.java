package com.ksoot.spark.common.error;

public interface ErrorType {

  ErrorType DEFAULT = JobErrorType.unknown();

  String code();

  String title();

  String message();
}
