## Set Up
1. Provision an Ubuntu 20.04 VM and connect to it (most linux distributions should work for the following).

2. Install Docker as the Container Runtime and then k3s as the Kubernetes Distribution (feel free to use every Kubernetes Flavour that can use docker as a runtime, 
using docker however is mandatory for the installation of our monitoring stack).
```
curl https://releases.rancher.com/install-docker/20.10.sh | sh (docker installation script)
docker -v
curl -sfL https://get.k3s.io | sh -s - server --docker
kubectl get nodes
kubectl config view --raw >~/.kube/config
chmod 600 ~/.kube/config
```
3. Install influxdb as our Timeseries Database, as collected Metrics will be saved here. This can be done on a sperate server, on the same server or also in the Kubernetes Cluster running in a container. 
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

5. Install the metrics-collector to your cluster and train a model. Make sure to modify the Environment Variables to your needs in the Deployment.yml. 
A detailed overview of the Options can be found here.
```
git clone https://github.com/lukonjun/container-anomaly-detection.git
kubectl apply -f container-anomaly-detection/kubernetes/Namespace.yml
kubectl apply -f container-anomaly-detection/kubernetes/metrics-collector/
```
