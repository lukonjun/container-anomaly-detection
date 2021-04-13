# Dashboard
Documentation from here: https://github.com/kubernetes/dashboard 
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.2.0/aio/deploy/recommended.yaml
kubectl create -f ClusterRoleBinding.yml -f ServiceAccount.yml
```

Otherwise you get this errors accessing resources over the dashboard
```bash
configmaps is forbidden: User "system:serviceaccount:kubernetes-dashboard:kubernetes-dashboard" cannot list resource "configmaps" in API group "" in the namespace "default"
```

open port to Kubernetes Api on local port
```bash
kubectl proxy
```

every service account has a generated token, get the token from secret and copy it
```bash
kubectl describe secret admin-user-token-rtrr7
```

Then you can sign in at this URL using your token we got in the previous step:
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
