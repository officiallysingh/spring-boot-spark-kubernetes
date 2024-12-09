package com.ksoot.spark.common.config.properties;

import static com.ksoot.spark.common.util.SparkOptions.Jdbc.FETCH_SIZE;
import static com.ksoot.spark.common.util.SparkOptions.Jdbc.ISOLATION_LEVEL;

import com.ksoot.spark.common.util.SparkOptions;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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
public class JdbcOptions {

  @NotEmpty private String url;

  @NotEmpty private String driver = "org.postgresql.Driver";

  @NotEmpty private String database;

  @NotEmpty private String username;

  @NotEmpty private String password;

  @NotNull private Integer fetchsize = 1000;

  @NotNull private Integer batchsize = 1000;

  @NotEmpty private String isolationLevel = "READ_UNCOMMITTED";

  public String getUrl() {
    return this.url + "/" + this.database;
  }

  public Properties connectionProperties() {
    Properties properties = new Properties();
    properties.put(SparkOptions.Jdbc.DRIVER, this.driver);
    properties.put(SparkOptions.Jdbc.USER, this.username);
    properties.put(SparkOptions.Jdbc.PASSWORD, this.password);
    return properties;
  }

  public Map<String, String> readOptions() {
    return Map.of(FETCH_SIZE, String.valueOf(this.fetchsize), ISOLATION_LEVEL, this.isolationLevel);
  }

  public Map<String, String> writeOptions() {
    return Map.of(FETCH_SIZE, String.valueOf(this.batchsize), ISOLATION_LEVEL, this.isolationLevel);
  }
}
