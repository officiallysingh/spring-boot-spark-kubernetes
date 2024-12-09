package com.ksoot.spark.common.executor;

public interface Executor<T, S> {

  S execute(final T request);
}
