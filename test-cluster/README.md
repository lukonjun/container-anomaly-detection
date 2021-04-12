# Cluster Set Up
Documentation from: https://rancher.com/docs/k3s/latest/en/quick-start/  
$ curl -sfL https://get.k3s.io | sh -
Get the kubeconfig file from /etc/rancher/k3s/k3s.yaml and copy to your local machine  
Make sure to also change the ip from localhost to the public ip of your server in your kubeconfig file
### kubectl Hacks
One liner to run a debug container
$ kubectl run -i --tty --rm debug --image=busybox --restart=Never -- sh  
Check endpoints of a service
$ kubectl get endpoint
### helm hacks
see the yamls and all the values that were actually deployed?
$ helm -n <namespace> get all <release-name>
