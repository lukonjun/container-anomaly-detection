# Cluster Set Up
### Installation
Offical Documentation here: https://rancher.com/docs/k3s/latest/en/installation/ha/  
First Create an External Datastore and also add a user
```bash
$ apt update
$ apt install mysql-server
$ mysql_secure_installation
```
Make sure to comment out *bind-address = 127.0.0.1*, otherwise we wont be able to connect from a remote server
Configure a proxy infront of the master server, the configuration can look like this
```nginx
load_module /usr/lib/nginx/modules/ngx_stream_module.so;

events {}

stream {
  upstream k3s_servers {
    server 162.55.52.153:6443;
    server 159.69.23.59:6443;
  }

  server {
    listen 6443;
    proxy_pass k3s_servers;
  }
}
```
Install on every Node Docker as a Container Runtime, telegraf is not able to fetch data from containerd
```bash
curl https://releases.rancher.com/install-docker/19.03.sh | sh
```
Adding a Master Node
```bash
$ K3S_DATASTORE_ENDPOINT='mysql://username:password@tcp(database_ip_or_hostname:port)/database'
$ curl -sfL https://get.k3s.io | sh -s - server --node-taint CriticalAddonsOnly=true:NoExecute --tls-san load_balancer_ip_or_hostname --docker
```
Get the kubeconfig file from /etc/rancher/k3s/k3s.yaml and copy it to your local kubeconfig    
Make sure to also change the ip from localhost to the ip of your load balancer. Afterwards run, to get running nodes
```bash
$ kubectl get no
```
Adding a Worker Node involves getting a Node token from one of the master nodes to join the cluster. This file is located under */var/lib/rancher/k3s/server/node-token*
```bash
curl -sfL https://get.k3s.io | K3S_URL=https://load_balancer_ip_or_hostname:6443 K3S_TOKEN=mynodetoken sh -s - --docker
```
You can start and stop the servcie with systemctl
```bash
systemctl status k3s-agent.service
```
To unistall k3s you can either use 
```bash
/usr/local/bin/k3s-uninstall.sh (or as k3s-agent-uninstall.sh
```
### kubectl Hacks
One liner to run a debug container, busybox only includes no curl, only wget
```bash
$ kubectl run -i --tty --rm debug --image=busybox --restart=Never -- sh  
```
Enter a pod over command line
```bash
$ kubectl exec -it test-pod /bin/sh
```
displays the endpoints of a service, if no endpoint is listed your label do not apply to any pod
```bash
$ kubectl get endpoint
```
access the kubernetes api with kubectl
```bash
$ kubectl get --raw /api/v1/nodes | jq
```
quickly run a container
```bash
$ kubectl run nginx --image=nginx
```
if manual deletion is to exhausting
```bash
$ for i in `kubectl get clusterrolebinding | awk '/46h/ {print $1}'`; do echo k delete clusterrolebindings $i; done
```
create a clusterrolebinding with existing service account and clusterrole
```bash
kubectl create clusterrolebinding pod-reader --clusterrole=pod-reader --serviceaccount=namespace:sa-name
```
debug what permissions a service account actually has
```bash
kubectl auth can-i <verb> <resources> --as=system:serviceaccount:<namespace>:<service account name>
kubectl auth can-i get pods --as=system:serviceaccount:playground:pod-watcher-release-chart-pod-watcher
```
pass kubeconfig file via command line
```bash
kubectl --kubeconfig ./admin.conf get nodes
```
### helm hacks
see the yamls and all the values (at the top) that were actually deployed
```bash
$ helm -n namespace get all release-name  
```
download a chart from a repository and unpack it in local directory
```bash
$ helm pull --untar bitnami/nginx 
```
search for a specific chart in all added repositories, leave out the search name to list all charts
```bash
$ helm search repo nginx
$ helm search repo
```
if your helm release is in a pending state (stuck), often caused by canceling a helm release with ctrl + c, rolling back to an earlier release could fix this
```bash
$ helm history helm-release
REVISION	UPDATED                 	STATUS         	CHART             	APP VERSION	DESCRIPTION             
1       	Mon Nov  9 14:57:36 2020	pending-install	generic-base-0.2.1	0.1.0      	Initial install underway

$ helm rollback helm-release 1
Rollback was a success! Happy Helming!

$ helm history helm-release
REVISION	UPDATED                 	STATUS         	CHART             	APP VERSION	DESCRIPTION             
1       	Mon Nov  9 14:57:36 2020	pending-install	generic-base-0.2.1	0.1.0      	Initial install underway
2       	Mon Nov  9 15:06:15 2020	deployed       	generic-base-0.2.1	0.1.0      	Rollback to 1  
```
