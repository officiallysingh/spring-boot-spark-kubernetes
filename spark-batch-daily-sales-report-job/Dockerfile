FROM officiallysingh/spark:3.5.3

ADD target/spark-batch-daily-sales-report-job-*.jar $SPARK_JOB_APPS_DIR/spark-batch-daily-sales-report-job.jar

ENTRYPOINT [ "/opt/entrypoint.sh" ]