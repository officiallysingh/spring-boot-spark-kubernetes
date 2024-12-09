package com.ksoot.spark.sales;

import com.ksoot.spark.common.error.ErrorType;

public enum SalesJobErrors implements ErrorType {
  INVALID_DATE(
      "dailysalesjob.invalid.date", "Invalid Request", "Job date: {0}, {1} can not be in future");

  private final String code;

  private final String title;

  private final String message;

  SalesJobErrors(final String code, final String title, final String message) {
    this.code = code;
    this.title = title;
    this.message = message;
  }

  @Override
  public String code() {
    return this.code;
  }

  @Override
  public String title() {
    return this.title;
  }

  @Override
  public String message() {
    return this.message;
  }
}
