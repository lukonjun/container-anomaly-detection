package de.lukonjun.metricscollector.data;

import de.lukonjun.metricscollector.controller.PodController;
import de.lukonjun.metricscollector.influxdb.InfluxController;
import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.model.*;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class DataAggregator {

    @Autowired
    PodController podController;

    @Autowired
    ApiConnection apiConnection;

    @Autowired
    InfluxController influxController;

    Logger logger = LoggerFactory.getLogger(DataAggregator.class);

    @Scheduled(fixedRateString = "10000")
    public void collect() throws IOException, ApiException, InterruptedException {

        // Connect to the Kubernetes Api
        ApiConnection.ApiConnectionPojo a = apiConnection.createConnection();

        // Get a List of Metrics already including {PodName,ContainerName,Image,ImageSizeBytes}
        List<Metrics2> metricsPojoList = podController.fillMetrics2(a.getClient(),a.getApi());

        // Connect to influxdb
        metricsPojoList.forEach(m ->{
            // kubernetes_pod_container
            String query = "SELECT * FROM kubernetes_pod_container WHERE container_name =\'" + m.getContainerName() +"\' AND pod_name =\'" + m.getPodName() + "\' Limit 1";
            System.out.print(query);
            List<KubernetesPodContainer> list = influxController.selectFromKubernetesPodContainer(query);
            KubernetesPodContainer k = list.get(0);
            m.setNamespace(k.getNamespace());
            m.setMemoryUsageBytes(k.getMemoryUsageBytes());
            m.setCpuUsageNanocores(k.getCpuUsageNanocores());
            m.setLogsfsUsedBytes(k.getLogsfsUsedBytes());
            m.setRootfsUsedBytes(k.getRootfsUsedBytes());
        });

        metricsPojoList.forEach(m ->{
            // kubernetes_pod_network
            String query = "SELECT * FROM kubernetes_pod_network WHERE pod_name =\'" + m.getPodName() + "\' Limit 1";
            System.out.print(query);
            List<KubernetesPodNetwork> list = influxController.selectFromKubernetesPodNetwork(query);
            KubernetesPodNetwork k = list.get(0);
            m.setRx_bytes(k.getRxBytes());
            m.setTx_bytes(k.getTxBytes());
        });

        metricsPojoList.forEach(m ->{
            // kubernetes_pod_volume
            String query = "SELECT * FROM kubernetes_pod_volume WHERE pod_name =\'" + m.getPodName() + "\' Limit 1";
            System.out.print(query);
            List<KubernetesPodVolume> list = influxController.selectFromKubernetesPodNVolume(query);
            KubernetesPodVolume k = list.get(0);
            m.setUsedBytesVolume(k.getUsedBytes());
        });

        metricsPojoList.forEach(m ->{
            // docker_container_blkio
            //String query = "SELECT * FROM docker_container_blkio WHERE io.kubernetes.container.name =\'" + m.getContainerName() +"\' AND io.kubernetes.pod.name =\'" + m.getPodName() + "\' Limit 1";
            String query = "SELECT * FROM docker_container_blkio LIMIT 10";
            System.out.print(query);
            List<DockerContainerBlkio> list = influxController.selectDockerContainerBlkio(query);
            DockerContainerBlkio k = list.get(0);
            m.setIoServiceRecursiveRead(k.getIoServiceRecursiveRead());
            m.setIoServiceRecursiveWrite(k.getIoServiceRecursiveWrite());
        });

        logger.info("Fetched metrics for " + metricsPojoList.size() + " containers");

        // (optional) Persist by writing to DB
    }

}
