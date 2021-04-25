# container-anomaly-detection
### Motivation
The TU Berlin DOS research Group created over the last several years different AI models for detecting anomalies of black-box IT-services in monitoring streams. When an anomaly is detected, it can trigger an alerting and an automatic or manual action can be taken to revert the component into a healthy state. Problematic is, that training this AI models takes significant time, which cannot be afforded, especially in the fast-living world of Kubernetes where Pods get restarted, scaled up and terminated in seconds. In order to reuse models, it is crucial to find the same type of Service or at least similar Service to apply historic trained ML models and avoid costly warm up phases. Aim of this Project will be to develop an Application capable of ranking black-box IT-services by their similarity within a given IT-infrastructure.
### Repository Overview
*/.github/workflows* automated image builds and push to container registry  
*/k3s*               cluster installation documentation and yamls  
*/metrics-collector* spring boot application for collection metrics from pods    
*/pod-watcher*       spring boot application for watching new spawning pods 
### Used Metrics
Metrics are polled by telegraf and then written into influxdb, this table gives an overview of the explicit metrics we are looking at  
Metric | Source | Example
-------- | --------  | --------  
pod_name   |  kubernetes_pod_container  | traefik-6f9cbd9bd4-rgh45
namespace   | kubernetes_pod_container  | kube-system
memory_usage_bytes   | kubernetes_pod_container | 831.488
cpu_usage_nanocores   | kubernetes_pod_container   | /metrics
imageSizeBytes   |  /api/v1/nodes/ (API)  | 326597
image   | ? | ?
rx_bytes (receive)   | kubernetes_pod_network | 1.774.377
tx_bytes (transmit)   | kubernetes_pod_network | 953.169
io_service_recursive_read   | docker_container_blkio | 8192
io_service_recursive_write   | docker_container_blkio | 0
