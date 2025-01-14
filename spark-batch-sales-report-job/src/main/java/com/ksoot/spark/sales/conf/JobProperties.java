package com.ksoot.spark.sales.conf;

import jakarta.validation.constraints.*;
import java.time.YearMonth;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
@ConfigurationProperties(prefix = "ksoot.job", ignoreInvalidFields = true)
public class JobProperties {

  /** Statement month * */
  @NotNull @PastOrPresent private YearMonth month = YearMonth.now();

  /** Unique correlation id for each Job execution. */
  @Size(min = 1, max = 50, message = "Correlation id should be between 2 and 50 characters")
  private String correlationId;
}
