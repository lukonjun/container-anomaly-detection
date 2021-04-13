# Cluster Set Up
### Installation
Documentation from: https://rancher.com/docs/k3s/latest/en/quick-start/  
```bash
$ curl -sfL https://get.k3s.io | sh -
```
Get the kubeconfig file from /etc/rancher/k3s/k3s.yaml and copy it to your local kubeconfig    
Make sure to also change the ip from localhost to the public ip of your server
### kubectl Hacks
One liner to run a debug container, busybox only includes no curl, only wget
```bash
$ kubectl run -i --tty --rm debug --image=busybox --restart=Never -- sh  
```
displays the endpoints of a service, if no endpoint is listed your label do not apply to any pod
```bash
$ kubectl get endpoint
```
access the kubernetes api with kubectl
```bash
$ kubectl get --raw /api/v1/nodes | jq
```
### helm hacks
see the yamls and all the values (at the top) that were actually deployed
```bash
$ helm -n namespace get all release-name  
```
if your helm release is in a pending state (stuck), oftend cause by canceling a helm release with ctrl + c, rolling back to an earlier release could fix this
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
