package com.ksoot.spark.conf;

import jakarta.validation.constraints.NotEmpty;
import java.util.Properties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
public class SparkJobProperties {

  @NotEmpty private String mainClassName;

  @NotEmpty private String jarFile;

  /**
   * Spark config properties for this job.
   *
   * @see <a
   *     href="https://spark.apache.org/docs/3.5.3/configuration.html#available-properties">Spark
   *     configurations</a>
   * @see <a
   *     href="https://spark.apache.org/docs/3.5.3/running-on-kubernetes.html#configuration">Spark
   *     Kubernetes configurations</a>
   */
  private Properties sparkConfig = new Properties();
}
