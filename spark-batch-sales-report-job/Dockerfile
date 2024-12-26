FROM ksoot/spark:3.5.3

ADD target/spark-batch-sales-report-job-*.jar $SPARK_JOB_APPS_DIR/spark-batch-sales-report-job.jar

ENTRYPOINT [ "/opt/entrypoint.sh" ]