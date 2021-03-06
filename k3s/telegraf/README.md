1. If not already done install helm
```bash
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
```
2. Add the helm repo
```bash
helm repo add influxdata https://helm.influxdata.com/  
helm pull --untar influxdata/telegraf-ds  
```
3. Set Up a new User in the influxdb database (if authentication is configured)
```bash
root@influxdb:~# influx -username <username>  -password '<mypassword>'
Connected to http://localhost:8086 version 1.8.5
InfluxDB shell version: 1.8.5
> CREATE DATABASE k3s_telegraf_ds
> USE k3s_telegraf_ds
Using database k3s_telegraf_ds
> CREATE USER user_k3s_telegraf_ds WITH PASSWORD '<mypassword>' WITH ALL PRIVILEGES
```
4. Overwrite Values in Helm Chart for the connection
```bash
vi telegraf-ds/values.yaml
- influxdb:
    urls:
        - "http://116.203.124.188:8086"
    database: "k3s_telegraf_ds"
    retention_policy: ""
    timeout: "5s"
    username: "user_k3s_telegraf_ds"
    password: "<mypassword>"
    user_agent: "telegraf"
    insecure_skip_verify: false
```
5. Overwrite some Configuration of the ConfigMap to access all monitoring Data, this step is important :bangbang:, if not changed the metrics-collector wont be able to fetch all metrics, and in its current implementation wont be able to create a training set. 
```
vi telegraf-ds/templates/configmap.yaml
    [[inputs.docker]]
    endpoint = "unix:///var/run/docker.sock"
    total = true
    total_include = ["cpu", "blkio", "network"]
```
6. create a namespace
```bash
kubectl create namespace telegraf-ds 
```
7. install helm chart
```bash
helm upgrade --install --namespace telegraf-ds --install telegraf-ds ./telegraf-ds
```
8. Check the logs of the Container, and also if data is written to influxdb.  
If you want to conntect influx to Grafana this is a great dashboard https://grafana.com/grafana/dashboards/9111
