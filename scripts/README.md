# Chart Script
Bash script used for installing charts into a Kubernetes Cluster
To run the script pass either install or uninstall
```
./charts install
```
Please specify Charts you want to install in the Label array
```
label_array=(mysql nginx)
```
the repository in the repo array
```
repo_array=(repo_url1 repo_url2)
```
and a name you can choose in the name array
```
name_array=(bitnami wso2 t3n mirantis shubhamtatvamasi)
```