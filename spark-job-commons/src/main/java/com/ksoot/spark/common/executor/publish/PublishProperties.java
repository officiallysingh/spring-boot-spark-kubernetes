package com.ksoot.spark.common.executor.publish;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.spark.sql.SaveMode;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
public class PublishProperties {

  /** Out file format, csv or parquet. Default: csv */
  @NotEmpty private String format = "csv";

  /** Output file path. Default: spark-output */
  @NotEmpty private String path = "spark-output";

  /**
   * Save mode for the output file. Options are Append, Overwrite, ErrorIfExists, Ignore. Default:
   * Overwrite
   */
  @NotNull private SaveMode saveMode = SaveMode.Overwrite;

  /** Whether to merge output into single file. Default: false */
  private boolean merge = false;
}
