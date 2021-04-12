# Harbor Container Registry
Helm Chart is available over aritact.io
$ helm repo add harbor https://helm.goharbor.io
$ kubectl create namespace harbor
$ kubens harbor
$ helm install harbor-release harbor/harbor
