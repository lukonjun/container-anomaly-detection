#!/bin/bash
kubectl apply -f /Users/lucasstocksmeier/Coding/container-anomaly-detection/kubernetes/pod-watcher/Deployment.yml
touch results.txt
echo "sleeping for 10 seconds"
sleep 10
echo "done"
name_podwatcher=`kubectl get pods | grep watcher | awk '{print $1}'`
echo "name  of the podwatcher pod $name_podwatcher"
echo "---" >> results.txt
# Monitor for 5 min
end=$((SECONDS+300))
while [ $SECONDS -lt $end ]; do
	cpu_usage=`kubectl top pod $name_podwatcher | grep m | awk '{print $2}'`
    # kubectl exec pod-watcher-797f88b59b-npfx8 -- top -b -d1 -n1 | grep CPU | awk '{print $2}' | head -n 1
    # echo "$cpu_usage"
    # echo "$SECONDS"
    echo "$SECONDS $cpu_usage" >> results.txt
    sleep 20
done
# Create 5 Pods
kubectl run --image=nginx test1234einname
kubectl run --image=traefik wasfaelltmirein
kubectl run --image=nginx dasgehtauchgut
kubectl run --image=traefik huhutdududututut

# Monitor again for 5 Minutes
end=$((SECONDS+300))
while [ $SECONDS -lt $end ]; do
	cpu_usage=`kubectl top pod $name_podwatcher | grep m | awk '{print $2}'`
    # kubectl exec pod-watcher-797f88b59b-npfx8 -- top -b -d1 -n1 | grep CPU | awk '{print $2}' | head -n 1
    # echo "$cpu_usage"
    # echo "$SECONDS"
    echo "$SECONDS $cpu_usage" >> results.txt
    sleep 20
done