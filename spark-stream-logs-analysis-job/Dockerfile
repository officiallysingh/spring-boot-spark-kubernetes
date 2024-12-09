FROM officiallysingh/spark:3.5.3

ADD target/spark-stream-logs-analysis-job-*.jar $SPARK_JOB_APPS_DIR/spark-stream-logs-analysis-job.jar

ENTRYPOINT [ "/opt/entrypoint.sh" ]