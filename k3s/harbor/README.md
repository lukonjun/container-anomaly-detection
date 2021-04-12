# Harbor Container Registry
Helm Chart is available over https://artifacthub.io/
$ helm repo add harbor https://helm.goharbor.io
$ kubectl create namespace harbor
$ kubens harbor
$ helm install harbor-release harbor/harbor
Check out ingress resources
$ kubectl get ingress -n harbor
Set local entries in your /etc/host file for notary.harbor.domain and core.harbor.domain to the ip of your master node  
Access https://core.harbor.domain/ the user credentials if not changed are admin/Harbor12345
