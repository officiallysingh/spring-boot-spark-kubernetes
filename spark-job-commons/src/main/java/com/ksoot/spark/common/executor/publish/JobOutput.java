package com.ksoot.spark.common.executor.publish;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

@Getter
@ToString
public class JobOutput {

  private final String location;

  private final List<String> files;

  private final String format;

  private JobOutput(final String location, final List<String> files, final String format) {
    Assert.hasText(location, "location is required");
    Assert.hasText(format, "format is required");
    this.location = location;
    this.files = Objects.nonNull(files) ? files : Collections.emptyList();
    this.format = format;
  }

  public static JobOutput empty(final String location, final String format) {
    return new JobOutput(location, null, format);
  }

  public static JobOutput of(final String location, final List<String> files, final String format) {
    return new JobOutput(location, files, format);
  }

  public boolean isEmpty() {
    return CollectionUtils.isEmpty(this.files);
  }

  public boolean hasFiles() {
    return CollectionUtils.isNotEmpty(this.files);
  }

  public boolean hasSingleFile() {
    return this.hasFiles() && this.files.size() == 1;
  }

  public boolean hasMultipleFiles() {
    return this.hasFiles() && this.files.size() > 1;
  }
}
