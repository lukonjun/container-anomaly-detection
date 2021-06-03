package de.lukonjun.metricscollector.data;

import de.lukonjun.metricscollector.controller.PodController;
import de.lukonjun.metricscollector.influxdb.InfluxController;
import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.ml.J48AnomalyDetector;
import de.lukonjun.metricscollector.ml.Sample;
import de.lukonjun.metricscollector.model.*;
import de.lukonjun.metricscollector.pojo.ContainerPojo;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.*;

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
            List<KubernetesPodVolume> list = influxController.selectFromKubernetesPodVolume(query);
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
            Instant startTime = null; // m.getStartTime();
            Instant currentTime = m.getTime();
            //https://stackoverflow.com/questions/55779996/calculate-days-hours-and-minutes-between-two-instants
            // Or use Duration
            Duration duration = Duration.between(currentTime, startTime);
            System.out.println(duration);
            System.out.println(m.getPodName() +" is running for seconds: " + duration.toSeconds());    // prints: 65

            m.setRunningTimeSeconds((int)duration.toSeconds());
            System.out.println(m.getPodName() +" running time is " + duration);
        });

        logger.info("Fetched metrics for " + metricsPojoList.size() + " containers");

        return metricsPojoList;
    }

    private List<Metrics2> filterRelevantPods(List<ContainerPojo> containerPojoList, List<Metrics2> metricsPojoList) {

        List<Metrics2> filteredMetricsList = new ArrayList<>();
        boolean foundMatch = false;
        for (Metrics2 m:metricsPojoList) {
            foundMatch = false;
            for(ContainerPojo c:containerPojoList){
                if (m.getPodName().equals(c.getPodName()) && m.getImageName().equals(c.getContainerName()) && m.getNamespace().equals(c.getNamespace())) {
                    filteredMetricsList.add(m);
                    m.setLabel(c.getLabel());
                    foundMatch = true;
                    //System.out.print("Found Match " + c.getPodName() + " " + c.getContainerName() + " " + c.getNamespace());
                    break;
                }

            }
            if(!foundMatch){
                //System.out.println("Found no Match for" + m.getPodName());
            }
        }

        return filteredMetricsList;
    }

    public List<Metrics2> getMetrics(int repetitions, List<String> labels) throws Exception {

        // Search for containers that match the labels
        List<ContainerPojo> containerPojoList = new ArrayList<>();
        List<Metrics2> metricsPojoList = podController.collectPodMetrics(labels);
        logger.info("Found " + metricsPojoList.size() + " Containers that match the labels " + labels.toString());
        // TODO DO we even need this?, as we alreday filter at a different place for the labels
        labels.forEach(label -> {
            for(Metrics2 m:metricsPojoList){
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
        int seconds = 10;
        List<Metrics2> listMetrics2s = new ArrayList<>();
        for(int i = 0; i < repetitions; i++){
            // Metrics need to be filtered in collect
            listMetrics2s.addAll(collect(containerPojoList));
            if(i != repetitions - 1) {
                logger.info("Sleeping for " + seconds);
                TimeUnit.SECONDS.sleep(seconds);
            }
            logger.info("Finished " + (i + 1) + " iteration");
        }
        logger.info("Successfully fetched " + listMetrics2s.size() + " metric points");

        // Train Model or write to a File

        return listMetrics2s;

    }

    @Scheduled(fixedRateString = "${influxdb.metrics.collection.rate:10000}")
    public void decisionTree() throws Exception {

        // How often to fetched Metrics, sleeps for 10 seconds between each iteration
        int fetchMetricsInterval = 1000;
        // Which labels should match when collecting container Metrics
        List<String> labels = new ArrayList<>();
        labels.add("mysql");
        labels.add("nginx");
        labels.add("mongodb");
        labels.add("postgresql");
        labels.add("apache");

        logger.info("Fetching Metrics for Containers with the name " + labels.toString() +  " for " + fetchMetricsInterval+ " Iterations");
        List<Metrics2> metrics2List = getMetricsTimeInterval(fetchMetricsInterval, labels);

        // Write File with custom method to container_metrics.arff
        // J48 modelFromFile = new CreateFile().testDataSet();

        // Create Trainings Sample
        List<Sample> trainingSamples = new ArrayList<>();
        metrics2List.forEach(m -> {
            Sample s = new Sample();
            trainingSamples.add(s);
            // podName,"namespace","memoryUsageBytes","cpuUsageNanocores",
            // "logsfsUsedBytes","rootfsUsedBytes","imageSizeBytes", "image"
            // "containerName","rx_bytes","tx_bytes", "ioServiceRecursiveRead",
            // "ioServiceRecursiveWrite","usedBytesVolume","runningTimeSeconds"
            double [] metricsArray = {
                    m.getPodName().hashCode(),m.getNamespace().hashCode(),m.getMemoryUsageBytes(),m.getCpuUsageNanocores(),
                    m.getLogsfsUsedBytes(), m.getRootfsUsedBytes(), m.getImageSizeBytes(), m.getImageName().hashCode(),
                    m.getContainerName().hashCode(), m.getRx_bytes(), m.getTx_bytes(), m.getIoServiceRecursiveRead(),
                    m.getIoServiceRecursiveWrite(), 0, m.getRunningTimeSeconds()
            };
            s.setMetricsArray(metricsArray);
            s.setLabel(m.getLabel());
        });
        logger.info("Generate Training Sample of size " + trainingSamples.size());

        J48AnomalyDetector j48AnomalyDetector = new J48AnomalyDetector();

        // Init weka model
        Instances instances = j48AnomalyDetector.createDataset(trainingSamples);
        j48AnomalyDetector.fillDataset(instances, trainingSamples);

        // write to file for comparison
        // From https://waikato.github.io/weka-wiki/formats_and_processing/save_instances_to_arff/
        String absolutePath = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/container_metrics_generated.arff";
        ArffSaver saver = new ArffSaver();
        saver.setInstances(instances);
        saver.setFile(new File(absolutePath));
        saver.writeBatch();
        logger.info("Write Test Sample in arff Format to File " + absolutePath);

        // Train weka model
        J48 wekaModel = new J48();

        // Custom Options
        /*
        String[] options = new String[4];
        options[0] = "-C";
        options[1] = "0.1";
        options[2] = "-M";
        options[3] = "2";
        wekaModel.setOptions(options);
        */

        wekaModel.buildClassifier(instances);

        System.out.println("Decision Tree");
        System.out.println(wekaModel.toString());

        logger.info("Test our created Model");
        double[] values = new double[]{
                -2045226423,-1594835516,8769536,0,32768,65536,132899597,-1866261913,104760218,998,42,256,0,0,129744
        };
        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(instances);
        String label = j48AnomalyDetector.inputInstanceIntoModel(wekaModel,instance);
        System.out.println("Nginx Pod get recognized by the model as: " + label);

    }

    //@Scheduled(fixedRateString = "1000")
    public void start() throws Exception {
        int seconds = 100;

        List<String> labels = new ArrayList<>();
        labels.add("mysql");
        labels.add("nginx");
        labels.add("mongodb");
        labels.add("postgresql");
        labels.add("apache");

        List<Metrics2> metrics2List = getMetricsTimeInterval(seconds,labels);

        logger.info("Generated List with Metircs of the last " + seconds + " seconds and a size of " + metrics2List.size() + " entries");

    }

    public List<Metrics2> getMetricsTimeInterval(int seconds, List<String> labels) throws Exception {

        List<Metrics2> listMetrics = new ArrayList<>();
        List<Metrics2> listMetricsDynamic = new ArrayList<>();
        // Get Pod Metrics that dont change over time
        List<Metrics2> listMetricsStable = collectStableMetrics(labels);

        for(Metrics2 m:listMetricsStable){
            listMetricsDynamic = collectDynamicMetrics(seconds,m);
            listMetrics.addAll(listMetricsDynamic);
        }

        // Return List of Stable and Dynamic Metrics
        return listMetrics;
    }

    private Set<String> setOfPodUids(List<Metrics2> metrics2List){
        Set<String>  setPodUids = new HashSet<>();
        for(Metrics2 m:metrics2List){
            setPodUids.add(m.getPodUid());
        }
        return setPodUids;
    }

    private List<Metrics2> collectDynamicMetrics(int seconds, Metrics2 m) throws Exception {

        List<Metrics2> dynamicMetrics = new ArrayList<>();

        List<Metrics2> dockerContainerBlkioMetrics = influxController.getMetricsFromDockerContainerBlkio(seconds,m);
        List<Metrics2> kubernetesPodContainerMetrics = influxController.getMetricsFromKubernetesPodContainer(seconds,m);
        List<Metrics2> kubernetesPodNetworkMetrics = influxController.getMetricsFromKubernetesPodNetwork(seconds,m);
        List<Metrics2> kubernetesPodVolumeMetrics = influxController.getMetricsFromKubernetesPodVolume(seconds,m);

        // get size of smallest list
        int sizeList1 = kubernetesPodNetworkMetrics.size() < kubernetesPodVolumeMetrics.size() ? kubernetesPodNetworkMetrics.size() : kubernetesPodVolumeMetrics.size();
        int sizeList2 = dockerContainerBlkioMetrics.size() < kubernetesPodContainerMetrics.size() ? dockerContainerBlkioMetrics.size() : kubernetesPodContainerMetrics.size();
        int sizeList = sizeList1 < sizeList2 ? sizeList1 : sizeList2;

        for(int i = 0; i < sizeList; i++){
            Metrics2 blkio = dockerContainerBlkioMetrics.get(i);
            Metrics2 podMetrics = kubernetesPodContainerMetrics.get(i);
            Metrics2 podNetwork = kubernetesPodNetworkMetrics.get(i);
            //Metrics2 podVolume = kubernetesPodVolumeMetrics.get(i);
            Metrics2 newMetricPoint = new Metrics2();
            // Metric
            newMetricPoint.setLabel(m.getLabel());
            newMetricPoint.setPodUid(m.getPodUid());
            newMetricPoint.setPodName(m.getPodName());
            newMetricPoint.setContainerName(m.getContainerName());
            newMetricPoint.setNamespace(m.getNamespace());
            newMetricPoint.setImageSizeBytes(m.getImageSizeBytes());
            newMetricPoint.setImageName(m.getImageName());
            //Time
            newMetricPoint.setStartTime(m.getStartTime());
            newMetricPoint.setTime(blkio.getTime());
            Duration duration = Duration.between(Instant.ofEpochSecond(m.getStartTime().getSeconds(),m.getStartTime().getNanos()), blkio.getTime());
            newMetricPoint.setRunningTimeSeconds((int)duration.toSeconds());
            // blkio
            newMetricPoint.setTime(blkio.getTime());
            newMetricPoint.setIoServiceRecursiveRead(blkio.getIoServiceRecursiveRead());
            newMetricPoint.setIoServiceRecursiveWrite(blkio.getIoServiceRecursiveWrite());
            // podMetrics
            newMetricPoint.setMemoryUsageBytes(podMetrics.getMemoryUsageBytes());
            newMetricPoint.setCpuUsageNanocores(podMetrics.getCpuUsageNanocores());
            newMetricPoint.setLogsfsUsedBytes(podMetrics.getLogsfsUsedBytes());
            newMetricPoint.setRootfsUsedBytes(podMetrics.getRootfsUsedBytes());
            dynamicMetrics.add(newMetricPoint);
            // podNetwork
            newMetricPoint.setTx_bytes(podNetwork.getTx_bytes());
            newMetricPoint.setRx_bytes(podNetwork.getRx_bytes());
            // podVolume
            //newMetricPoint.setUsedBytesVolume(podVolume.getUsedBytesVolume());
            logger.info("blkio time: " + blkio.getTime() + "podMetrics time: " + podMetrics.getTime()+ " podNetwork: " + podNetwork.getTime() + " podVolume: not defined atm");
        }

        return dynamicMetrics;
    }

    private List<Metrics2> collectStableMetrics(List<String> labels) throws Exception {
        return podController.collectPodMetrics(labels);
    }

    public void writingToFile(List<Metrics2> metrics2List) throws InterruptedException, ApiException, IOException {

        String absolutePath = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/container_metrics.arff";
        File file = new File(absolutePath);

        FileWriter fileWriter = new FileWriter(file);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("@relation container_metrics");
        printWriter.println("");
        printWriter.println("@attribute runningTimeSeconds numeric");
        printWriter.println("@attribute imageSizeBytes numeric");
        printWriter.println("@attribute cpuUsageNanocores numeric");
        printWriter.println("@attribute memoryUsageBytes numeric");
        printWriter.println("@attribute label {mysql, nginx, mongodb, postgresql, apache}");
        printWriter.println("");
        printWriter.println("@data");

        metrics2List.forEach(m ->{
            // pod_name runningTimeSeconds imageSizeBytes cpuUsageNanocores memoryUsageBytes label
            printWriter.println(m.getRunningTimeSeconds() + "," + m.getImageSizeBytes() + "," +
                    m.getCpuUsageNanocores() + "," + m.getMemoryUsageBytes() + "," + m.getLabel());
        });

        printWriter.close();
        System.out.println("Absolute Path of File " + file.getAbsolutePath());
    }

}