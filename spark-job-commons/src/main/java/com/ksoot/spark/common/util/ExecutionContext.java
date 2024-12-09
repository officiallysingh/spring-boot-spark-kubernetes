package com.ksoot.spark.common.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
public class ExecutionContext {

  private final Map<String, Object> resources;

  private ExecutionContext(final boolean threadSafe) {
    this.resources = threadSafe ? new ConcurrentHashMap<>() : new HashMap<>();
  }

  public static ExecutionContext newInstance() {
    return new ExecutionContext(false);
  }

  /**
   * Get a value identified by given <code>key</code>.
   *
   * @param key Resource key
   * @return Resource value, or <code>null</code> if not found
   */
  public Object get(final String key) {
    Assert.hasText(key, "Resource key must be not null");
    return this.resources.get(key);
  }

  /**
   * Get a value identified by given <code>key</code>.
   *
   * @param key Resource key
   * @return Resource value type casted to type, or empty <code>Optional</code> if not found
   * @throws ClassCastException if the value is not of the expected type
   */
  public <T> Optional<T> get(final String key, Class<T> type) {
    Assert.hasText(key, "Resource key must be not null");
    Object obj = this.resources.get(key);
    if (type.isInstance(obj)) {
      return Optional.of(type.cast(obj));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Bind the given resource key with the specified value
   *
   * @param key Resource key (not null)
   * @param resource instance. If <code>null</code>, the mapping will be removed
   * @return Previous bound instance, if any
   */
  public <T> T put(final String key, final T resource) {
    Assert.hasText(key, "key must be not null");

    Object previous;
    if (resource != null) {
      previous = this.resources.put(key, resource);
    } else {
      previous = this.resources.remove(key);
    }
    return (T) previous;
  }

  /**
   * If the specified key is not already associated with a value (or is mapped to null), attempts to
   * compute its value using the given mapping function and enters it into this map unless null
   *
   * @param key
   * @param mappingFunction
   * @return
   * @param <T>
   */
  public <T> T computeIfAbsent(String key, Function<String, T> mappingFunction) {
    return (T) this.resources.computeIfAbsent(key, mappingFunction);
  }

  /**
   * Bind the given resource key with the specified value, if not already bound
   *
   * @param key Resource key (not null)
   * @param resource instance
   * @return Previous value, or <code>null</code> if no value was bound and the new instance it's
   *     been mapped to the key
   */
  public <T> T putIfAbsent(final String key, final T resource) {
    Assert.hasText(key, "Resource key must be not null");
    if (resource != null) {
      Object previous = this.resources.putIfAbsent(key, resource);
      return (T) previous;
    }
    return null;
  }

  /**
   * Removes the resource instance bound to given key
   *
   * @param key Resource key (not null)
   * @return <code>true</code> if found and removed
   */
  public boolean remove(final String key) {
    Assert.hasText(key, "Resource key must be not null");
    Object removed = this.resources.remove(key);

    if (removed != null) {
      log.debug("Removed resource with key [" + key + "]");
    }

    return removed != null;
  }

  /**
   * Check if resource instance exists for given key
   *
   * @param key Resource key (not null)
   * @return <code>true</code> if found otherwise <code>false</code>
   */
  public boolean contains(final String key) {
    return this.resources.containsKey(key);
  }

  /** Clears all resource bindings */
  public void clear() {
    this.resources.clear();

    log.debug("Context cleared");
  }
}
