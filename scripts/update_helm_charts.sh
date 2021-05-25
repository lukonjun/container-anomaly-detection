#!/bin/bash
name_array=(bitnami wso2 t3n mirantis shubhamtatvamasi)
repo_array=(https://charts.bitnami.com/bitnami https://helm.wso2.com https://storage.googleapis.com/t3n-helm-charts https://charts.mirantis.com https://shubhamtatvamasi.github.io/helm)
label_array=(mysql nginx)
namespace=helm-charts

if ! command -v helm &> /dev/null
then
    echo "helm could not be found, please install it"
    exit 1
fi

echo "add helm repositories"
total=5
for (( i=0; i<=$(( $total -1 )); i++ ))
do 
    helm repo add "${name_array[$i]}" "${repo_array[$i]}"
done

echo "update helm repositories"
helm repo update

if ! command -v kubectl &> /dev/null
then
    echo "kubectl could not be found, please install it"
    exit 1
fi

RESULT=$(command kubectl get ns | grep ${namespace})
if [ "$RESULT" == "" ]
then
    echo "create namespace $namespace"
    kubectl create namespace $namespace
fi

lenght_name_array=${#name_array[@]}
lenght_label_array=${#label_array[@]}
for (( i=0; i<=$(( $lenght_name_array -1 )); i++ ))
do
    for (( l=0; l<=$(( $lenght_label_array -1 )); l++ ))
    do  
        OUTPUT=$(ls -la)
        test=$(helm search repo ${name_array[$i]}/${label_array[$l]})
        echo $test
        echo $OUTPUT
        if  == "No results found"
        then
            echo "Hi"
        fi
    done  
done

wait
# Mysql
echo "install bitnami/mysql"
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install my-release bitnami/mysql
echo "install wso2/mysql"
helm repo add wso2 https://helm.wso2.com
helm install my-mysql wso2/mysql --version 1.6.9
echo "install wso2/mysql-am"
helm repo add wso2 https://helm.wso2.com
helm install my-mysql-am wso2/mysql-am --version 4.0.0-1
echo "install t3n/mysql"
helm repo add t3n https://storage.googleapis.com/t3n-helm-charts
helm install my-mysql-t3n t3n/mysql --version 1.0.0

# nginx
helm repo add bitnami https://charts.bitnami.com/bitnami
echo "install bitnami/nginx"
helm install my-nginx-bitnami bitnami/nginx --version 8.9.0

helm repo add mirantis https://charts.mirantis.com
echo "install mirantis/nginx"
helm install my-nginx-mirantis mirantis/nginx --version 0.1.0

helm repo add shubhamtatvamasi https://shubhamtatvamasi.github.io/helm
echo "install shubhamtatvamasi/nginx"
helm install my-nginx-shubhamtatvamasi shubhamtatvamasi/nginx --version 0.1.12

echo "install falco nginx"
helm install t3n-nginx t3n/nginx