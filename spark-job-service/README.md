# Spring boot service to trigger spark jobs via spark-submit

Run [**`SparkJobService`**](src/main/java/com/ksoot/spark/SparkJobService.java) as Spring boot application.
> [!IMPORTANT]  
> Make sure environment variable `SPARK_HOME` is set to local spark installation.  
> Make sure environment variable `M2_REPO` is set to local maven repository.

## In profile `local`
Set active profile `local`.

## In profile `minikube`
Set active profile `local`.

**To connect with Redis, ArangoDB, MongoDB and Kafka running on local machine**, outside docker/minikube,  
do following changes to make these services accessible from application running as docker container inside minikube.
* **Redis**: change `bind 0.0.0.0` and `protected-mode no` in redis configuration file `redis.conf`.
* **ArangoDB**: change `endpoint = tcp://0.0.0.0:8529` in arango configuration file `arangod.conf`.
* **MongoDB**: change `bindIp: 0.0.0.0` in mongo configuration file `mongod.conf`.
* **Kafka**: change `listeners=PLAINTEXT://0.0.0.0:9092` in kafka configuration file `server.properties`.
* Get master ip address and port using command `kubectl cluster-info` and update it in [`application-minikube.properties`](src/main/resources/config/application-minikube.yml) file.  
For example, if master url is `https://127.0.0.1:62112`, configurations would be as follows.
```yaml
spark:
  master: k8s://https://127.0.0.1:62112
```
* Get **your machine ip** address using command `ipconfig getifaddr en0` and update it in [`application-minikube.properties`](src/main/resources/config/application-minikube.yml) file.  
For example, if your machine ip address is `192.168.1.6`, configurations would be as follows.
```yaml
spark:
  driver:
    extraJavaOptions: >
      -DMONGODB_URL=mongodb://192.168.1.6:27017
      -DMONGO_FEATURE_DB=feature-repo
```

## Requirements

This application requires:

- Java 17
- Spark 3.4.1
- `maven`
- `docker`

## References
* [**`Running Spark on Kubernetes`**](https://spark.apache.org/docs/3.4.1/running-on-kubernetes.html#cluster-mode).
* [**`Spark configurations`**](https://spark.apache.org/docs/3.4.1/configuration.html#available-properties).
* [**`Spark Kubernetes configurations`**](https://spark.apache.org/docs/3.4.1/running-on-kubernetes.html#configuration).

## Commands

Go to project root directory in terminal.

### Prepare
```shell
minikube start
kubectl create serviceaccount spark
kubectl create clusterrolebinding spark-role --clusterrole=edit --serviceaccount=default:spark --namespace=default
```

### Run
```shell
chmod +x spark-job-submit.sh

mvn clean install

docker image build . -t spark-job-service:0.0.1  -f Dockerfile --no-cache
minikube image load spark-job-service:0.0.1

kubectl apply -f k8s/fabric8-rbac.yaml
kubectl apply -f k8s/deployment.yaml

kubectl port-forward <pod name> 8090:8090
```

### Inspect
```shell
kubectl get pods
kubectl logs <pd name>
kubectl describe pod <pd name>

docker run -it --entrypoint /bin/sh spark-job-service:0.0.1
find / -name "*.jar"
```

### Cleanup
```shell
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/fabric8-rbac.yaml

kubectl delete pods --all
kubectl delete pods --all -n default
kubectl delete pod <pod name>

minikube image rm spark-job-service:0.0.1

docker rmi spark-job-service:0.0.1

minikube delete
```
