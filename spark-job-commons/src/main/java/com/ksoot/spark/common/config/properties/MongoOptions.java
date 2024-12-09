package com.ksoot.spark.common.config.properties;

import com.ksoot.spark.common.util.SparkOptions;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
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
public class MongoOptions {

  /** MongoDB URL to connect to. Default: mongodb://localhost:27017 */
  @NotEmpty private String url = "mongodb://localhost:27017";

  /** Features Database name. */
  @NotEmpty private String database;

  public Map<String, String> options(final String collection) {
    return Map.of(
        SparkOptions.Mongo.READ_CONNECTION_URI, this.url,
        SparkOptions.Mongo.DATABASE, this.database,
        SparkOptions.Mongo.COLLECTION, collection);
  }
}
