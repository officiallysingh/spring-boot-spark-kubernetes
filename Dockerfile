ARG java_image_tag=17-jammy
FROM eclipse-temurin:${java_image_tag}

ARG spark_uid=185

ENV SPARK_HOME /opt/spark
ENV SPARK_JOB_APPS_DIR /opt/spark/job-apps

RUN groupadd --system --gid=${spark_uid} spark && \
    useradd --system --uid=${spark_uid} --gid=spark spark

RUN set -ex; \
    apt-get update; \
    apt-get install -y gnupg2 wget bash tini libc6 libpam-modules krb5-user libnss3 procps net-tools gosu libnss-wrapper; \
    mkdir -p /opt/spark; \
    mkdir /opt/spark/python; \
    mkdir -p /opt/spark/examples; \
    mkdir -p "$SPARK_JOB_APPS_DIR"; \
    mkdir -p /opt/spark/work-dir; \
    chmod g+w /opt/spark/work-dir; \
    touch /opt/spark/RELEASE; \
    chown -R spark:spark /opt/spark; \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su; \
    rm -rf /var/lib/apt/lists/*

# Install Apache Spark
ENV SPARK_TGZ_URL=https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz \
    SPARK_TGZ_ASC_URL=https://archive.apache.org/dist/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz.asc \
    GPG_KEY=0A2D660358B6F6F8071FD16F6606986CF5A8447C

RUN set -ex; \
    export SPARK_TMP="$(mktemp -d)"; \
    cd $SPARK_TMP; \
    wget -nv -O spark.tgz "$SPARK_TGZ_URL"; \
    wget -nv -O spark.tgz.asc "$SPARK_TGZ_ASC_URL"; \
    export GNUPGHOME="$(mktemp -d)"; \
    gpg --batch --keyserver hkps://keys.openpgp.org --recv-key "$GPG_KEY" || \
    gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys "$GPG_KEY"; \
    gpg --batch --verify spark.tgz.asc spark.tgz; \
    gpgconf --kill all; \
    rm -rf "$GNUPGHOME" spark.tgz.asc; \
    tar -xf spark.tgz --strip-components=1; \
    chown -R spark:spark .; \
#    Remove any spark jars that may be conflict with the ones in your application dependencies.
    rm -f jars/protobuf-java-2.5.0.jar; \
    rm -f jars/guava-14.0.1.jar; \
    rm -f jars/HikariCP-2.5.1.jar; \
    mv jars /opt/spark/; \
    mv RELEASE /opt/spark/; \
    mv bin /opt/spark/; \
    mv sbin /opt/spark/; \
    mv kubernetes/dockerfiles/spark/entrypoint.sh /opt/; \
    mv kubernetes/dockerfiles/spark/decom.sh /opt/; \
    mv examples /opt/spark/; \
    ln -s "$(basename /opt/spark/examples/jars/spark-examples_*.jar)" /opt/spark/examples/jars/spark-examples.jar; \
    mv kubernetes/tests /opt/spark/; \
    mv data /opt/spark/; \
    chmod a+x /opt/decom.sh; \
    chmod a+x /opt/entrypoint.sh; \
    cd ..; \
    rm -rf "$SPARK_TMP";

WORKDIR /opt/spark/work-dir
RUN chmod g+w /opt/spark/work-dir
RUN chmod a+x /opt/decom.sh

# Default to user spark
USER ${spark_uid}