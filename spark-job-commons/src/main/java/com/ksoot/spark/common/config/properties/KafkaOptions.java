package com.ksoot.spark.common.config.properties;

import static com.ksoot.spark.common.util.SparkOptions.Kafka.*;

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
public class KafkaOptions {

  /** Kafka Bootstrap servers. Default: localhost:9092 */
  @NotEmpty private String bootstrapServers = "localhost:9092";

  /** Kafka topic where to write output. */
  private String topic;

  /** Kafka Key serializer type. Default: org.apache.kafka.common.serialization.StringSerializer */
  @NotEmpty private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";

  /** Kafka val serializer type. Default: org.apache.kafka.common.serialization.StringSerializer */
  @NotEmpty
  private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";

  @NotEmpty private String failOnDataLoss = "true";

  public Map<String, String> writeOptions() {
    return Map.of(
        BOOTSTRAP_SERVERS,
        this.bootstrapServers,
        TOPIC,
        this.topic,
        KEY_SERIALIZER,
        this.keySerializer,
        VALUE_SERIALIZER,
        this.valueSerializer);
  }

  public Map<String, String> readOptions() {
    return Map.of(BOOTSTRAP_SERVERS, this.bootstrapServers, FAIL_ON_DATA_LOSS, this.failOnDataLoss);
  }
}
