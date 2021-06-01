package de.lukonjun.metricscollector.data;

import de.lukonjun.metricscollector.CreateFile;
import de.lukonjun.metricscollector.controller.PodController;
import de.lukonjun.metricscollector.influxdb.InfluxController;
import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.ml.J48AnomalyDetector;
import de.lukonjun.metricscollector.ml.Sample;
import de.lukonjun.metricscollector.model.*;
import de.lukonjun.metricscollector.pojo.ContainerPojo;
import de.lukonjun.metricscollector.pojo.MetricsPojo;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.unsupervised.attribute.StringToNominal;

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

    public List<Metrics2> getMetrics(int repetitions, List<String> labels) throws IOException, ApiException, InterruptedException {

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
        int fetchMetricsInterval = 10;
        // Which labels should match when collecting container Metrics
        List<String> labels = new ArrayList<>();
        labels.add("mysql");
        labels.add("nginx");
        labels.add("mongodb");
        labels.add("postgresql");
        labels.add("apache");

        logger.info("Fetching Metrics for Containers with the name " + labels.toString() +  " for " + fetchMetricsInterval+ " Iterations");
        List<Metrics2> metrics2List = getMetrics(fetchMetricsInterval, labels);

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
                    m.getIoServiceRecursiveWrite(), m.getUsedBytesVolume(), m.getRunningTimeSeconds()
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

    }

    //@Scheduled(fixedRateString = "10000")
    public List<Metrics2> getMetricsTimeInterval(int seconds, List<String> labels) throws Exception {

        // Get Pod Metrics that dont change over time
        List<Metrics2> listMetricsStable = collectStableMetrics(labels);

        // Get Pod Metrics that change over time
        List<Metrics2> listMetricsDynamic = collectDynamicMetrics(seconds, labels, setOfPodUids(listMetricsStable));

        List<Metrics2> listMetrics = new ArrayList<>();

        // Write Stable Metrics into Dynamic Metrics
        listMetricsDynamic.forEach(dynamicMetric -> {
            for(Metrics2 stableMetric:listMetricsStable){
                if(stableMetric.getPodName().equals(dynamicMetric.getPodName()) && stableMetric.getNamespace().equals(dynamicMetric.getNamespace())){
                    // transferValues
                    listMetrics.add(dynamicMetric);
                    break;
                }
            }
        });

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

    private List<Metrics2> collectDynamicMetrics(int seconds, List<String> labels, Set<String> setPodUids) throws Exception {

        List<Metrics2> dynamicMetrics = new ArrayList<>();
        // Get Metrics for every Table
        // What do we do if size of lists differ? Do we need to increase the timeout to inlfux if query take longer?
        // Write Values from Influx to Metrics that let us compare if it matches to the labels set
        List<Metrics2> dockerContainerBlkioMetrics = influxController.getMetricsFromDockerContainerBlkio(seconds,labels, setPodUids);
        List<Metrics2> kubernetesPodContainerMetrics = influxController.getMetricsFromKubernetesPodContainer(seconds,labels);
        List<Metrics2> kubernetesPodNetworkMetrics = influxController.getMetricsFromKubernetesPodNetwork(seconds,labels);
        List<Metrics2> kubernetesPodVolume = influxController.getMetricsFromKubernetesPodVolume(seconds,labels);
        int listLenght = dockerContainerBlkioMetrics.size();
        if(listLenght != kubernetesPodContainerMetrics.size() || listLenght != kubernetesPodNetworkMetrics.size() || listLenght != kubernetesPodVolume.size()){
            throw new Exception("List size of dynamic lists is not equal");
        }
        // Check the Time and Put the Metrics in One Object, if not matching, ignore
        for(int i = 0; i < dockerContainerBlkioMetrics.size(); i++){
            Metrics2 m = new Metrics2();

        }
        // Return List
        return null;
    }

    private List<Metrics2> collectStableMetrics(List<String> labels) throws IOException, ApiException {
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