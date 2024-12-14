#!/bin/bash

set -e

BROKER="localhost:19092"
KAFKA_TOPICS_CMD="/opt/kafka/bin/kafka-topics.sh" # Update this path as per your Kafka image
TOPICS=("error_logs" "audit_logs" "activity_events")

for TOPIC in "${TOPICS[@]}"; do
  echo "Checking if topic $TOPIC exists..."
  if $KAFKA_TOPICS_CMD --list --bootstrap-server "$BROKER" | grep -q "^${TOPIC}$"; then
    echo "Topic $TOPIC already exists, skipping creation."
  else
    echo "Creating topic: $TOPIC"
    $KAFKA_TOPICS_CMD --create \
      --bootstrap-server "$BROKER" \
      --replication-factor 1 \
      --partitions 3 \
      --topic "$TOPIC"
    echo "Topic $TOPIC created successfully."
  fi
done