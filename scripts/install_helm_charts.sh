#!/bin/sh
namespace = playground
if []
then
fi


if [ $1 == uninstall ]
then
    echo Hey that\'s a large number.
    pwd
fi

helm repo update

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


