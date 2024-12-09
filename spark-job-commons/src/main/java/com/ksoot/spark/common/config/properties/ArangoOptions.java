package com.ksoot.spark.common.config.properties;

import com.ksoot.spark.common.util.SparkOptions;
import java.time.Duration;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.hadoop.shaded.org.checkerframework.checker.index.qual.Positive;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Validated
public class ArangoOptions {

  /** List of ArangoDB endpoints, Default: localhost:8529 */
  private String[] endpoints = {"localhost:8529"};

  /** ArangoDB database name, Default: _system */
  private String database = "_system";

  /** ArangoDB username, Default root: */
  private String username = "root";

  /** ArangoDB password, Default: "" */
  private String password = "";

  /** Enable SSL for ArangoDB connection, Default: false */
  private boolean sslEnabled;

  /** ArangoDB SSL certificate value, Default: "" */
  private String sslCertValue = "";

  /**
   * Cursor time to live in seconds, see the ISO 8601 standard for java.time.Duration String
   * patterns, Default: 30 seconds
   */
  @Positive private Duration cursorTtl = Duration.ofSeconds(30);

  public long cursorTtl() {
    return this.cursorTtl.toSeconds();
  }

  public String endpoints() {
    return String.join(",", this.endpoints);
  }

  public Map<String, String> options() {
    return Map.of(
        SparkOptions.Common.FORMAT, SparkOptions.Arango.FORMAT,
        SparkOptions.Arango.ENDPOINTS, this.endpoints(),
        SparkOptions.Arango.DATABASE, this.database,
        SparkOptions.Arango.USERNAME, this.username,
        SparkOptions.Arango.PASSWORD, this.password,
        SparkOptions.Arango.SSL_ENABLED, String.valueOf(this.sslEnabled),
        SparkOptions.Arango.SSL_CERT_VALUE, this.sslCertValue,
        SparkOptions.Arango.CURSOR_TIME_TO_LIVE, String.valueOf(this.cursorTtl()));
  }
}
