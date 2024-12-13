#!/bin/bash
set -e

# Wait for Kafka to start
sleep 10

# Create the topic - error_logs
kafka-topics --create \
    --bootstrap-server localhost:19092 \
    --replication-factor 1 \
    --partitions 3 \
    --topic error_logs

# Create the topic - job-stop-requests
kafka-topics --create \
    --bootstrap-server localhost:19092 \
    --replication-factor 1 \
    --partitions 3 \
    --topic job-stop-requests

# Allow Kafka to continue running
exec "$@"