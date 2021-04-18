kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
k port-forward argocd-server-859b4b5578-42lqm 9000:8080
kubectl get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 --decode
