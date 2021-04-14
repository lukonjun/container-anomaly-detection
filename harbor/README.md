# Harbor Installation

Prerequiste is a Server with 4GB Ram and 2 CPUs, docker and docker-compose. Im installing this on a Centos 8 Server
Official Documentation: https://goharbor.io/docs/1.10/install-config/

Install docker with the docker install script from rancher
$ curl https://releases.rancher.com/install-docker/19.03.sh | sh
$ docker -v

Install docker-compose
$ curl -L "https://github.com/docker/compose/releases/download/1.23.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
$ chmod +x /usr/local/bin/docker-compose
$ docker-compose --version

Install Certbot and request Certificate
$ dnf install epel-release -y
$ dnf install certbot -y
$ certbot certonly -d container-registry.lukonjun.de
Certs and private key are located under 
/etc/letsencrypt/live/container-registry.lukonjun.de/fullchain.pem
/etc/letsencrypt/live/container-registry.lukonjun.de/privkey.pem
Set up a Cron Job to automatically renew the cert
$ echo "0 0,12 * * * root python3 -c 'import random; import time; time.sleep(random.random() * 3600)' && certbot renew -q" | sudo tee -a /etc/crontab > /dev/null
To manually renew your certificates run
$ certbot renew

Download the Harbor 
$ dnf install wget -y
$ wget https://github.com/goharbor/harbor/releases/download/v2.2.1/harbor-online-installer-v2.2.1.tgz
$ tar xvf harbor-online-installer-v2.2.1.tgz

Fill out the neccessary configuration values in harbor.yml.tmpl. 
hostname: container-registry.lukonjun.de
certificate: /etc/letsencrypt/live/container-registry.lukonjun.de/fullchain.pem
private_key: /etc/letsencrypt/live/container-registry.lukonjun.de/privkey.pem
harbor_admin_password:  password 
Then rename the file and run the installer script
$ mv harbor.yml.tmpl harbor.yml
$ ./install.sh --with-chartmuseum
Now watch for the containers to get in a healthy state
$ watch docker ps

Access Harbor via https://container-registry.lukonjun.de/ and log in with the credentials defined in the harbor.yml

first login to harbor, from your local machine
docker login container-registry.lukonjun.de
Username: admin
Password:
Login Succeeded
Then try to push an image to harbor from you local machine
docker pull nginx
docker tag nginx:latest container-registry.lukonjun.de/library/nginx:latest
docker push container-registry.lukonjun.de/library/nginx:latest
