package com.ksoot.spark.common.util;

import org.springframework.core.NestedRuntimeException;

public class StreamRetryableException extends NestedRuntimeException {
  public StreamRetryableException(String message, Throwable cause) {
    super(message, cause);
  }

  public StreamRetryableException(String message) {
    super(message);
  }
}
