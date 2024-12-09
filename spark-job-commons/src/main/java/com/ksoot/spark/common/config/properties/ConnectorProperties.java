package com.ksoot.spark.common.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.streaming.OutputMode;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
public class ConnectorProperties {

  @NotEmpty private String checkpointLocation = "spark-space/checkpoints";

  /**
   * Save mode for the output file. Applicable only to Batch writers. Options are Append, Overwrite,
   * ErrorIfExists, Ignore. Default: Overwrite
   */
  @NotNull private SaveMode saveMode = SaveMode.Overwrite;

  /**
   * Output mode for the output file. Applicable only to Stream writers. Options are Append,
   * Complete, Update. Default: Append
   */
  @NotNull private String outputMode = "Append";

  public OutputMode outputMode() {
    return switch (this.outputMode) {
      case "Append" -> OutputMode.Append();
      case "Complete" -> OutputMode.Complete();
      case "Update" -> OutputMode.Update();
      default ->
          throw new IllegalStateException(
              "Unexpected 'mlhb.ejestion.output-mode' value: " + this.outputMode);
    };
  }

  private FileOptions fileOptions = new FileOptions();

  private JdbcOptions jdbcOptions = new JdbcOptions();

  private MongoOptions mongoOptions = new MongoOptions();

  private ArangoOptions arangoOptions = new ArangoOptions();

  private KafkaOptions kafkaOptions = new KafkaOptions();
}
