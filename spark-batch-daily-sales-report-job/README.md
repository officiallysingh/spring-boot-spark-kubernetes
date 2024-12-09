# Daily sales Report Job implemented as Spring Cloud Task

Run [**`DailySalesReportJob`**](src/main/java/com/ksoot/spark/sales/DailySalesReportJob.java) as Spring boot application.

> [!IMPORTANT]  
> Run in active profile `local`.  
> Set VM argument `--add-exports java.base/sun.nio.ch=ALL-UNNAMED`
> Make sure environment variable `SPARK_HOME` is set to local spark installation.  
> Make sure environment variable `M2_REPO` is set to local maven repository.  

## Requirements
This application requires:

- Java 17
- Spark 3.5.1
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

Find k8s-apiserver-host and k8s-apiserver-port as follows.
```shell
kubectl cluster-info
```
Example output:
```shell
Kubernetes control plane is running at https://127.0.0.1:50003
CoreDNS is running at https://127.0.0.1:50003/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```

> [!IMPORTANT]
> As per above result `master` is set to `k8s://https://127.0.0.1:50003` in following `spark-submit` examples.  
> Change `master` in `spark-submit` command as given by `kubectl cluster-info`

### Test Minikube setup by running examples bundled in Spark distribution
> [!NOTE]
> `officiallysingh/spark:3.4.1` Custom spark image for Spark version 3.4.1 and Java 17 publicly available on Dockerhub.

```shell
minikube image load officiallysingh/spark:3.4.1

./bin/spark-submit \
    --master k8s://https://127.0.0.1:50003 \
    --deploy-mode cluster \
    --name spark-pi \
    --class org.apache.spark.examples.SparkPi \
    --conf spark.kubernetes.container.image=officiallysingh/spark:3.4.1 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.driverEnv.SPARK_USER=spark \
    --conf spark.executor.instances=2 \
    local:///opt/spark/examples/jars/spark-examples_2.13-3.4.1.jar
```
Check pods
```shell
kubectl get pods
```
While running:
```shell
NAME                               READY   STATUS    RESTARTS   AGE
spark-pi-004fdb9035f60cdd-exec-1   1/1     Running   0          4s
spark-pi-004fdb9035f60cdd-exec-2   1/1     Running   0          4s
spark-pi-c3ff5c9035f4cc9f-driver   1/1     Running   0          86s
```
On successful completion:
```shell
NAME                               READY   STATUS      RESTARTS   AGE
spark-pi-c3ff5c9035f4cc9f-driver   0/1     Completed   0          2m2s
```

### Run
> [!NOTE]
> `spark-word-count-job.jar` is already bundled in docker image `spark-word-count-job:0.0.1` at path `/opt/spark/job-apps/`.

```shell
mvn clean install

docker image build . -t spark-statement-job:0.0.1  -f Dockerfile --no-cache
minikube image load spark-statementjob:0.0.1

./bin/spark-submit \
    --master k8s://https://127.0.0.1:50003 \
    --deploy-mode cluster \
    --name spark-statement-job \
    --class com.ksoot.spark.statement.DailySalesReportJob \
    --conf spark.kubernetes.container.image=spark-statement-job:0.0.1 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.driverEnv.SPARK_USER=spark \
    --conf spark.executor.instances=2 \
    local:///opt/spark/job-apps/spark-statement-job.jar
```

### Inspect
```shell
kubectl get pods
kubectl logs <pd name>
kubectl describe pod <pd name>

docker run -it --entrypoint /bin/sh spark-statement-job:0.0.1
find / -name "*.jar"
```

### Cleanup
```shell
kubectl delete pods --all
kubectl delete pods --all -n default
kubectl delete pod <pod name>

minikube image rm spark-statement-job:0.0.1

docker rmi spark-statement-job:0.0.1

minikube delete
```

## Providing Job jar via mount volume, without building docker image

* Create a folder `kubevol/spark-apps` in $HOME directory.
* Copy `spark-statement-job` jar in this folder.
* Mount this folder in minikube and do `spark-submit` as follows.

```shell
echo $HOME
minikube mount $HOME/kubevol/spark-apps:/tmp/spark-apps

minikube ssh
ls -ld /tmp/spark-apps
ls /tmp/spark-apps

./bin/spark-submit \
    --master k8s://https://127.0.0.1:50003 \
    --deploy-mode cluster \
    --name spark-statement-job \
    --class com.ksoot.spark.statement.DailySalesReportJob \
    --conf spark.kubernetes.container.image=spark-statement-job:0.1.1 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.driverEnv.SPARK_USER=spark \
    --conf spark.executor.instances=2 \
    --conf spark.kubernetes.file.upload.path=/tmp/spark-apps \
    --conf spark.kubernetes.driver.volumes.hostPath.spark-host-mount.mount.path=/tmp/spark-apps \
    --conf spark.kubernetes.driver.volumes.hostPath.spark-host-mount.options.path=/tmp/spark-apps \
    --conf spark.kubernetes.executor.volumes.hostPath.spark-host-mount.mount.path=/tmp/spark-apps \
    --conf spark.kubernetes.executor.volumes.hostPath.spark-host-mount.options.path=/tmp/spark-apps \
    local:///tmp/spark-apps/spark-statement-job.jar
```