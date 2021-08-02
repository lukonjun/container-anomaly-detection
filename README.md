# container-anomaly-detection
### Motivation
The TU Berlin DOS research Group created over the last several years different AI models for detecting anomalies of black-box IT-services in monitoring streams. When an anomaly is detected, it can trigger an alerting and an automatic or manual action can be taken to revert the component into a healthy state. Problematic is, that training this AI models takes significant time, which cannot be afforded, especially in the fast-living world of Kubernetes where Pods get restarted, scaled up and terminated in seconds. In order to reuse models, it is crucial to find the same type of Service or at least similar Service to apply historic trained ML models and avoid costly warm up phases. Aim of this Project will be to develop an Application capable of ranking black-box IT-services by their similarity within a given IT-infrastructure.
### Pod Watcher
Key | Value | Description
---- | ----- | ---------
kubernetes.api.endpoint | https://kubernetes.default.svc | 
kubernetes.connect.from.outside | true | Set to false if running inside a Kubernetes Cluster
influxdb.server.url | http://116.203.124.188:8086 | 
influxdb.username | pod_metrics |
influxdb.password | mypassword |
influxdb.database | k3s_telegraf_ds |
data.aggregator.decision.tree.classifier.list | mysql,nginx | list of classifiers for our model, needs to match the classifiers of the trained model
timeout.fetching.metrics | 120000 | timeout for a thread to fetch metrics for a container, after the timeout is exceeded the thread finishes
path.serialized.model | /tmp/model | Path to the File in the Container where the serialized model is read 

### Metrics Collector
Key | Value | Description
---- | ----- | ---------
kubernetes.api.endpoint | https://kubernetes.default.svc | 
kubernetes.connect.from.outside | true | Set to false if running inside a Kubernetes Cluster
influxdb.server.url | http://116.203.124.188:8086 | 
influxdb.username | pod_metrics |
influxdb.password | mypassword |
influxdb.database | k3s_telegraf_ds |
data.aggregator.decision.tree.interval | 60000 | training interval in milli seconds
data.metrics.fetch.interval.seconds | 600 | fetching Data of the Last 600 seconds
data.aggregator.decision.tree.classifier.list | mysql,nginx,mongodb,postgresql,apache | list of classifiers for our model, influences which pod metrics we evaluate for our models
training.iterations | 1 | how many iterations of training before Calculating the Confusion Matrix and other Performance Measurements of the Model
training.ratio | 0.1 | Ratio of Validation vs MOdel Training, here 0.1 stand for 10% of the Metrics are used for Training and 90% for validation
training.maxNumber | 200 |train with 1 - X Datasets, starting with 1,2,3 and so on, used in Graph Plotting Method

### Repository Overview
*/.github/workflows* automated image builds and push to container registry  
*/k3s*               cluster installation documentation and yamls  
*/metrics-collector* spring boot application for collection metrics from pods    
*/pod-watcher*       spring boot application for watching new spawning pods 
### Used Metrics
Most Metrics are polled by telegraf and then written into influxdb, this table gives an overview of the explicit metrics we are looking at  
Metric | Source | Example
-------- | --------  | --------  
pod_name   |  kubernetes_pod_container  | traefik-6f9cbd9bd4-rgh45
namespace   | kubernetes_pod_container  | kube-system
memory_usage_bytes   | kubernetes_pod_container | 831.488
cpu_usage_nanocores   | kubernetes_pod_container   | 1.039.125
logsfs_used_bytes | kubernetes_pod_container  | 32.768
rootfs_used_bytes | kubernetes_pod_container  | 49.152
imageSizeBytes   |  /api/v1/nodes/ (API)  | 326597
image   | ? | ?
rx_bytes (receive)   | kubernetes_pod_network | 1.774.377
tx_bytes (transmit)   | kubernetes_pod_network | 953.169
io_service_recursive_read   | docker_container_blkio | 8192
io_service_recursive_write   | docker_container_blkio | 0
used_bytes | kubernetes_pod_volume  | 8.192
