# Harbor Container Registry
Helm Chart is available over https://artifacthub.io/  
```bash
$ helm repo add harbor https://helm.goharbor.io
$ kubectl create namespace harbor  
$ kubens harbor
$ helm install harbor-release harbor/harbor
```
Check out ingress resources
```
$ kubectl get ingress -n harbor
```
Set local entries in your /etc/host file for notary.harbor.domain and core.harbor.domain to the ip of your master node or proxy. Access https://core.harbor.domain/, the default user credentials are admin/Harbor12345
```bash
$ docker pull nginx
$ docker tag nginx:latest core.harbor.domain/library/nginx:latest
$ docker image ls | grep core  
```
When we try to push an image to harbor, we get the following error
```bash
$ docker push core.harbor.domain/library/nginx
Using default tag: latest
The push refers to repository [core.harbor.domain/library/nginx]
Get https://core.harbor.domain/v2/: x509: certificate signed by unknown authority
```
We can either let the docker engine trust the certificate, or create a valid one that is singed by a trusted CA
    
TODO
For the Future we should register a certificate trusted by lets encrypt, but for now we can let the docker daemon trust this certificate
Go to the Server to /var/lib/rancher/k3s/server/tls
There is a file client-ca.crt which we need to trust
To display the output run openssl x509 -text -noout -in client-ca.crt
openssl s_client -showcerts -connect core.harbor.domain:443 > cert
