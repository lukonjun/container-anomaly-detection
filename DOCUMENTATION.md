## Set Up
1. Provision an Ubuntu 20.04 VM and connect to it (most linux distributions should work for the following).

2. Install Docker as the Container Runtime and then k3s as the Kubernetes Distribution (feel free to use every Kubernetes Flavour that can use docker as a runtime, 
using docker however is mandatory for the installation of our monitoring stack).
```bash
curl https://releases.rancher.com/install-docker/20.10.sh | sh (docker installation script)
docker -v
curl -sfL https://get.k3s.io | sh -s - server --docker
kubectl get nodes
kubectl config view --raw >~/.kube/config
chmod 600 ~/.kube/config
```
3. Install influxdb as our Timeseries Database. This can be done on a sperate server, on the same server or also in the Kubernetes Cluster running in a container.
For convenience I am going to install it on the same server, however all options should work. This [Guide](https://computingforgeeks.com/install-influxdb-on-ubuntu-and-debian/) gives instructions on the installation. 
This process can be a bit tricky, here is a part of my configuration.
```
vi /etc/influxdb/influxdb.conf
[http]
  # Determines whether HTTP endpoint is enabled.
  enabled = true

  # Determines whether the Flux query endpoint is enabled.
  # flux-enabled = false

  # Determines whether the Flux query logging is enabled.
  # flux-log-enabled = false

  # The bind address used by the HTTP service.
  bind-address = ":8086"

  # Determines whether user authentication is enabled over HTTP/HTTPS.
  auth-enabled = true
```
4. Install telegraf, an agent who collects data, and connects to influxdb. Follow the Steps described [here](https://github.com/lukonjun/container-anomaly-detection/tree/main/k3s/telegraf).

5. Install the metrics-collector to your cluster and train a model (this Step is only necessary if you want to train your own model). Make sure to modify the Environment Variables to your needs in the Deployment.yml. 
A detailed overview of the Application Properties can be found [here](https://github.com/lukonjun/container-anomaly-detection#metrics-collector).
```bash
git clone https://github.com/lukonjun/container-anomaly-detection.git
kubectl apply -f container-anomaly-detection/kubernetes/Namespace.yml
kubectl apply -f container-anomaly-detection/kubernetes/metrics-collector/
```
6. Pay special attention to the property `data.aggregator.decision.tree.classifier.list`. This list specifies the classifiers for the training and also determines which Pod Metrics are used in the Training Set and ultimately for the training of the Model. For every Pod it gets checked if the PodName contains the Label. To make schedule lots of pods easier i have written a shell script that can deploy several pods, this might help and can be found [here](https://github.com/lukonjun/container-anomaly-detection/tree/main/scripts). If you train a model, your pods need to have a Name that matches an entry in the classifier List ⚠️. 
```java
private String containsLabel(V1.Pod pod, List<String> labels) {
    for (String label : labels) {
        if (pod.getMetadata().getName().contains(label)) {
            return label;
        }
    }
    return null;
}
``` 
7. If the training succeds you will see In the console output a Path to the file of the serialized model. Complete the following steps to able to reuse the model for the pod watcher. 
```bash
kubectl logs metrics-collector-74ff6d4db4-8gtts | grep path
kubectl exec -it metrics-collector-74ff6d4db4-8gtts /bin/sh
base64 tmpSerialized_Model  | tr -d \\n (Copy in an Editor, dont copy the / at the end)
```
8. Install the pod-watcher to your cluster and **insert if you have trained a model your base64 decoded String into the ConfigMap.yml**. If not you can you take the default yaml which includes a default model trained for mysql,nginx,apache,postgres and mongo.   
For every new spawning Pod a thread is started that trys to gather Data, the timeout of every Thread can be set via `timeout.fetching.metrics`. View again the Properties of the Container for special configuration and adapt in the Deployment.yml. Again make sure that the classifier List you specify `data.aggregator.decision.tree.classifier.list` match the Model you provide in the ConfigMap ⚠️. Otherwise this will lead to confusing false results.
```bash
git clone https://github.com/lukonjun/container-anomaly-detection.git
kubectl apply -f container-anomaly-detection/kubernetes/Namespace.yml
kubectl apply -f container-anomaly-detection/kubernetes/pod-watcher/
kubectl get pods | grep pod-watcher
kubectl logs pod-watcher
```
9. Start a new Pod `kubectl run nginx1239841 --image=nginx` and watch the logs. If everything is set up correctly it should look like this.
```
kubectl logs pod-watcher-6c4694bc84-jhpk2 -f
2021-08-06 09:24:42.962  INFO 1 --- [   scheduling-1] d.l.podwatcher.controller.PodController  : Start watching pods
Recognized Pod nginx1239841
Thread nginx1239841 running
2021-08-06 09:24:47.938  INFO 1 --- [   scheduling-1] d.l.podwatcher.controller.PodController  : Start watching pods
...
2021-08-06 09:25:37.927  INFO 1 --- [   scheduling-1] d.l.podwatcher.controller.PodController  : Start watching pods
nginx1239841 got classified by the model as nginx
Thread nginx1239841 finished
```
