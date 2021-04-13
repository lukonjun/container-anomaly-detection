# Dashboard
Documentation from here: https://github.com/kubernetes/dashboard 
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.2.0/aio/deploy/recommended.yaml
kubectl create -f ClusterRoleBinding.yml -f ServiceAccount.yml
```

We need to create a service account with cluster-admin permissions. Otherwise, if we for example log in with the default service account, we get this errors accessing resources over the dashboard, as we do not have the correct permissions.
```bash
configmaps is forbidden: User "system:serviceaccount:kubernetes-dashboard:kubernetes-dashboard" cannot list resource "configmaps" in API group "" in the namespace "default"
```
Open port to Kubernetes Api on local port
```bash
kubectl proxy
```
Every service account has a generated token, get the token from secret and copy it
```bash
kubectl describe secret admin-user-token-rtrr7
```
Then you can sign in at this URL using your token we got in the previous step:
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
