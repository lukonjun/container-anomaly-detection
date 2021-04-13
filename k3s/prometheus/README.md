# Prometheus
I followed the installtion steps on https://artifacthub.io/packages/helm/prometheus-community/kube-prometheus-stack  
```bash
$ helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
$ helm repo update
$ helm install prometheus prometheus-community/kube-prometheus-stack -n prometheus
```
This takes some time, afterwards try to connect via portforwarding to grafana, the default credentials are admin/prom-operator
```bash
$ kubectl port-forward deployment/prometheus-grafana 3000
```
We can also access the prometheus ui
```bash
kubectl port-forward prometheus-prometheus-kube-prometheus-prometheus-0 9090
```
