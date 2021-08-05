# container-anomaly-detection
## Directory Structure
```
├── k3s                        # Kubernetes Cluster and Services Installation Documentation
├── kubernetes                 # Manifests for Kubernetes Objects
├── metrics-collector          # Java Spring Boot Application for Model Training with Metrics
├── pod-watcher                # Java Spring Boot Application for Classification of Containers
└── scripts                    # Shell Scripts for starting Containers to easily train a Model
```
## Motivation
The TU Berlin DOS research Group created over the last several years different AI models for detecting anomalies of black-box IT-services in monitoring streams. Problematic is, that training this AI models takes significant time, which cannot be afforded, especially in the fast-living world of Kubernetes where Pods get restarted, scaled up and terminated in seconds. In order to **reuse models**, it is crucial to find the **same type of Service** or at least a similar Service to apply historic trained ML models and avoid costly warm up phases.   
## Solution
This Repository provides two Images, one for training ([metrics-collector](https://github.com/lukonjun/container-anomaly-detection/pkgs/container/metrics-collector)) and one for classifying new spawning containers ([pod-watcher](https://github.com/lukonjun/container-anomaly-detection/pkgs/container/metrics-collector)). Kurze Beschreibung was passiert. A detailed [documentation](https://github.com/lukonjun/container-anomaly-detection/blob/main/DOCUMENTATION.md) on how to set up the training container aswell as the pod-watcher can be found on the top level. 
### Metrics Collector
Following Application Properties can be set for the metrics-collector. Please specify the Properties you want to overwrite via Environment Variables in the [Depyloment Manifest](https://github.com/lukonjun/container-anomaly-detection/blob/main/kubernetes/metrics-collector/Deployment.yml).
Key | Value | Description
---- | ----- | ---------
kubernetes.api.endpoint | https://kubernetes.default.svc | Specify differently if the API can not be reached via the default service
kubernetes.connect.from.outside | true | Set to false if running inside a Kubernetes Cluster
influxdb.server.url | http://116.203.124.188:8086 | 
influxdb.username | pod_metrics |
influxdb.password | mypassword |
influxdb.database | k3s_telegraf_ds |
data.aggregator.decision.tree.interval | 60000 | training interval in milli seconds
data.metrics.fetch.interval.seconds | 600 | fetching Data of the Last 600 seconds
data.aggregator.decision.tree.classifier.list | mysql,nginx | list of classifiers for our model, influences which pod metrics we evaluate for our models
training.iterations | 1 | how many iterations of training before Calculating the Confusion Matrix and other Performance Measurements of the Model
training.ratio | 0.1 | Ratio of Validation vs MOdel Training, here 0.1 stand for 10% of the Metrics are used for Training and 90% for validation
training.maxNumber | 200 |train with 1 - X Datasets, starting with 1,2,3 and so on, used in Graph Plotting Method

### Pod Watcher
Following Application Properties can be set for the pod-watcher. Please specify the Properties you want to overwrite via Environment Variables in the [Depyloment Manifest](https://github.com/lukonjun/container-anomaly-detection/blob/main/kubernetes/pod-watcher/Deployment.yml).
Key | Value | Description
---- | ----- | ---------
kubernetes.api.endpoint | https://kubernetes.default.svc | Specify differently if the API can not be reached via the default service
kubernetes.connect.from.outside | true | Set to false if running inside a Kubernetes Cluster
influxdb.server.url | http://116.203.124.188:8086 | 
influxdb.username | pod_metrics |
influxdb.password | mypassword |
influxdb.database | k3s_telegraf_ds |
data.aggregator.decision.tree.classifier.list | mysql,nginx | list of classifiers for our model, needs to match exactly the classifiers list of the trained model
timeout.fetching.metrics | 120000 | timeout for a thread to fetch metrics for a container, after the timeout is exceeded the thread finishes
path.serialized.model | /tmp/model | Path to the File in the Container where the serialized model is read 
### Used Metrics
Most Metrics are polled by telegraf and then written into influxdb, some are also collected via the Kubernetes API. This table gives an overview of the explicit metrics we can use to train our ML Model. In the current metrics-collector Image all metrics are used, however if you inspect the source code you can set a filter to change this behaviour.
Metric | Source | Example Value
-------- | --------  | --------  
pod_name   |  influx kubernetes_pod_container  | traefik-6f9cbd9bd4-rgh45
namespace   | influx kubernetes_pod_container  | kube-system
memory_usage_bytes   | influx kubernetes_pod_container | 831.488
cpu_usage_nanocores   | influx kubernetes_pod_container   | 1.039.125
logsfs_used_bytes | influx kubernetes_pod_container  | 32.768
rootfs_used_bytes | influxkubernetes_pod_container  | 49.152
imageSizeBytes   |  API /api/v1/nodes/  | 326597
image   | API | bitnami/mysql
rx_bytes (receive)   | influx kubernetes_pod_network | 1.774.377
tx_bytes (transmit)   | influx kubernetes_pod_network | 953.169
io_service_recursive_read   | influx docker_container_blkio | 8192
io_service_recursive_write   | influx docker_container_blkio | 0
used_bytes | influx kubernetes_pod_volume  | 8.192
