#!/bin/bash

# Define the Spark submit command
SPARK_SUBMIT_CMD=${JOB_SUBMIT_COMMAND}

# Execute the Spark submit command
echo "Inside spark-job-submit.sh ..."
#echo "$SPARK_SUBMIT_CMD"

eval $SPARK_SUBMIT_CMD

# Check the exit status of the Spark submit command
if [ $? -eq 0 ]; then
  echo "Spark submit command executed successfully."
else
  echo "Spark submit command failed."
  exit 1
fi