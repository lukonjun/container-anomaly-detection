package de.lukonjun.metricscollector.data;

import de.lukonjun.metricscollector.controller.PodController;
import de.lukonjun.metricscollector.influxdb.InfluxController;
import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.model.*;
import de.lukonjun.metricscollector.pojo.ContainerPojo;
import de.lukonjun.metricscollector.pojo.MetricsPojo;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DataAggregator {

    @Autowired
    PodController podController;

    @Autowired
    ApiConnection apiConnection;

    @Autowired
    InfluxController influxController;

    Logger logger = LoggerFactory.getLogger(DataAggregator.class);

    public List<Metrics2> collect(List<ContainerPojo> containerPojoList) throws IOException, ApiException, InterruptedException {

        String collectionInterval = "60s";
        //WHERE time > now() -5m

        // Connect to the Kubernetes Api
        ApiConnection.ApiConnectionPojo a = apiConnection.createConnection();

        // Get a List of Metrics already including {PodName,ContainerName,Image,ImageSizeBytes}
        List<Metrics2> metricsPojoList = podController.fillMetrics2(a.getClient(),a.getApi());

        //filter only relevant pods
        metricsPojoList = filterRelevantPods(containerPojoList, metricsPojoList);

        // Connect to influxdb
        metricsPojoList.forEach(m ->{
            // kubernetes_pod_container
            String query = "SELECT * FROM kubernetes_pod_container WHERE container_name =\'" + m.getContainerName() +"\' AND pod_name =\'" + m.getPodName() + "\' Limit 1";
            //System.out.print(query);
            List<KubernetesPodContainer> list = influxController.selectFromKubernetesPodContainer(query);
            KubernetesPodContainer k = list.get(0);
            m.setTime(k.getTime());
            m.setNamespace(k.getNamespace());
            m.setMemoryUsageBytes(k.getMemoryUsageBytes());
            m.setCpuUsageNanocores(k.getCpuUsageNanocores());
            m.setLogsfsUsedBytes(k.getLogsfsUsedBytes());
            m.setRootfsUsedBytes(k.getRootfsUsedBytes());
        });

        metricsPojoList.forEach(m ->{
            // kubernetes_pod_network
            String query = "SELECT * FROM kubernetes_pod_network WHERE pod_name =\'" + m.getPodName() + "\' Limit 1";
            //System.out.print(query);
            List<KubernetesPodNetwork> list = influxController.selectFromKubernetesPodNetwork(query);
            KubernetesPodNetwork k = list.get(0);
            m.setRx_bytes(k.getRxBytes());
            m.setTx_bytes(k.getTxBytes());
        });

        metricsPojoList.forEach(m ->{
            // kubernetes_pod_volume
            String query = "SELECT * FROM kubernetes_pod_volume WHERE pod_name =\'" + m.getPodName() + "\' Limit 1";
            //System.out.print(query);
            List<KubernetesPodVolume> list = influxController.selectFromKubernetesPodNVolume(query);
            KubernetesPodVolume k = list.get(0);
            m.setUsedBytesVolume(k.getUsedBytes());
        });

        metricsPojoList.forEach(m ->{
            // docker_container_blkio
            //String query = "SELECT * FROM docker_container_blkio WHERE io.kubernetes.container.name =\'" + m.getContainerName() +"\' AND io.kubernetes.pod.name =\'" + m.getPodName() + "\' Limit 1";
            String query = "SELECT * FROM docker_container_blkio LIMIT 10";
            //System.out.print(query);
            List<DockerContainerBlkio> list = influxController.selectDockerContainerBlkio(query);
            DockerContainerBlkio k = list.get(0);
            m.setIoServiceRecursiveRead(k.getIoServiceRecursiveRead());
            m.setIoServiceRecursiveWrite(k.getIoServiceRecursiveWrite());
        });

        // Running Time
        // actual time - creationTimestamp": "2021-05-21T09:08:27Z"
        metricsPojoList.forEach(m ->{
            Instant startTime = m.getStartTime();
            Instant currentTime = m.getTime();
            //https://stackoverflow.com/questions/55779996/calculate-days-hours-and-minutes-between-two-instants
            // Or use Duration
            Duration duration = Duration.between(currentTime, startTime);
            //System.out.println(duration);
            //System.out.println(m.getPodName() +" is running for seconds: " + duration.toSeconds());    // prints: 65

            m.setRunningTimeSeconds((int)duration.toSeconds());
            //System.out.println(m.getPodName() +" running time is " + duration);
        });

        logger.info("Fetched metrics for " + metricsPojoList.size() + " containers");

        return metricsPojoList;
    }

    private List<Metrics2> filterRelevantPods(List<ContainerPojo> containerPojoList, List<Metrics2> metricsPojoList) {

        List<Metrics2> filteredMetricsList = new ArrayList<>();

        for (Metrics2 m:metricsPojoList) {
            containerPojoList.forEach(c -> {
                if (m.getPodName().equals(c.getPodName())
                        && m.getContainerName().equals(c.getContainerName())
                        && m.getNamespace().equals(c.getNamespace())) {
                    filteredMetricsList.add(m);
                    m.setLabel(c.getLabel());
                }
            });

        }

        return filteredMetricsList;
    }

    @Scheduled(fixedRateString = "10000")
    public List<Metrics2> getMetrics() throws IOException, ApiException, InterruptedException {

        List<String> labels = new ArrayList<>();
        labels.add("mysql");
        labels.add("nginx");

        // Search for containers that match the labels
        List<ContainerPojo> containerPojoList = new ArrayList<>();
        List<MetricsPojo> metricsPojoList = podController.collectPodMetrics();

        labels.forEach(label -> {
            for(MetricsPojo m:metricsPojoList){
                if(m.getPodName().contains(label)&&m.getContainerName().contains(label)){
                    ContainerPojo c = new ContainerPojo();
                    c.setPodName(m.getPodName());
                    c.setContainerName(m.getContainerName());
                    c.setNamespace(m.getNamespace());
                    c.setLabel(label);
                    containerPojoList.add(c);
                }
            }
        });

        // Collect Data for x min
        int repetitions = 4;
        int seconds = 10;
        List<Metrics2> listMetrics2s = new ArrayList<>();
        for(int i = 0; i < repetitions; i++){
            // Metrics need to be filtered in collect
            listMetrics2s.addAll(collect(containerPojoList));
            logger.info("Sleeping for " + seconds);
            TimeUnit.SECONDS.sleep(seconds);
        }
        logger.info("Successfully fetched " + listMetrics2s.size() + " metric points");

        // Train Model or write to a File

        return listMetrics2s;

    }
}