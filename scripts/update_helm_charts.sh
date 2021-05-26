#!/bin/bash
name_array=(bitnami wso2 t3n mirantis shubhamtatvamasi)
repo_array=(https://charts.bitnami.com/bitnami https://helm.wso2.com https://storage.googleapis.com/t3n-helm-charts https://charts.mirantis.com https://shubhamtatvamasi.github.io/helm)
label_array=(mysql nginx)
namespace=helm-charts

echo -n "Do you want to install or uninstall charts?"
read VAR

if ! command -v helm &> /dev/null
then
    echo "helm could not be found, please install it"
    exit 1
fi

if ! command -v kubectl &> /dev/null
then
    echo "kubectl could not be found, please install it"
    exit 1
fi

### Install Part
if [[ "$VAR" == "install" ]]
then

    echo "update helm repositories"
    helm repo update

    echo "add helm repositories"
    total=5
    for (( i=0; i<=$(( $total -1 )); i++ ))
    do 
        helm repo add "${name_array[$i]}" "${repo_array[$i]}"
    done

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
            helm_search=$(helm search repo ${name_array[$i]}/${label_array[$l]})
            if [ "$helm_search" != "No results found" ]
            then
                echo Running helm install --namespace $namespace release-${name_array[$i]}-${label_array[$l]} ${name_array[$i]}/${label_array[$l]}
                command helm install --namespace $namespace release-${name_array[$i]}-${label_array[$l]} ${name_array[$i]}/${label_array[$l]} > /dev/null
            fi
        done
    done

### Uninstall Part
elif [[ "$VAR" == "uninstall" ]]
then

    lenght_name_array=${#name_array[@]}
    lenght_label_array=${#label_array[@]}
    for (( i=0; i<=$(( $lenght_name_array -1 )); i++ ))
    do
        for (( l=0; l<=$(( $lenght_label_array -1 )); l++ ))
        do  
            echo Running helm uninstall --namespace $namespace release-${name_array[$i]}-${label_array[$l]}
            command helm uninstall --namespace $namespace release-${name_array[$i]}-${label_array[$l]} > /dev/null 2>&1
        done
    done

else
  echo "No valid input, please specify install or uninstall"
fi