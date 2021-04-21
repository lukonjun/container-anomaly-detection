# container-anomaly-detection
*/.github/workflows* automated image builds and push to container registry  
*/k3s*               cluster installation documentation and yamls  
*/metrics-collector* spring boot application for collection metrics from pods    
*/pod-watcher*       spring boot application for watching new spawning pods  

Metric | Example Value | Api Call
-------- | --------  | --------  
pod_name   | traefik-6f9cbd9bd4-rgh45   | /api/v1/pods
namespace   | kube-system      | /api/v1/namespaces
memoryBytes   | 15462400   | /metrics
cpu   | 0.00370346   | /metrics
sizeBytes   | 326597    | /api/v1/nodes/
image   | container-registry.lukonjun.de/library/metrics-collector@sha256:66313d4c22bf41a5abb6c5a900c19b68c6c980741a62868d796035bd42de17a9 | /api/v1/nodes/
container_network_receive_bytes_total   | 9.89698389e+08 1619030322817     | /api/v1/nodes/worker01/proxy/metrics/probes
container_network_transmit_bytes_total   | 825359 1619030325076   | /api/v1/nodes/worker01/proxy/metrics/probes
container_fs_writes_bytes_total   | 7.93698304e+08 1619030326733    | /api/v1/nodes/worker01/proxy/metrics/probes
container_fs_reads_bytes_total   | 5.4119424e+07 1619030326256    | /api/v1/nodes/worker01/proxy/metrics/probes
