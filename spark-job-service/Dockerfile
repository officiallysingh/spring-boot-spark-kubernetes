FROM officiallysingh/spark:3.5.3

USER root

ADD target/spark-job-trigger-service-*.jar /app.jar
ADD spark-job-submit.sh $SPARK_HOME/bin/spark-job-submit.sh
RUN chmod +x $SPARK_HOME/bin/spark-job-submit.sh

USER spark

EXPOSE 8090
ENTRYPOINT ["java","-jar","/app.jar"]