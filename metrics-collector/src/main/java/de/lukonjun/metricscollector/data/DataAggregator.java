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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.*;
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

    @Value("${data.aggregator.decision.tree.metrics.interval.seconds:120}")
    private int fetchMetricsInterval;

    @Value("#{'${data.aggregator.decision.tree.labels.list}'.split(',')}")
    private List<String> labels;

    Logger logger = LoggerFactory.getLogger(DataAggregator.class);

    @Scheduled(fixedRateString = "${data.aggregator.decision.tree:10000}")
    public void decisionTree() throws Exception {

        logger.info("Fetching Metrics for Containers with the name " + labels.toString() +  " for " + fetchMetricsInterval+ " Iterations");
        List<Metrics> metricsList = getMetricsTimeInterval(this.fetchMetricsInterval, this.labels);

        // Create Trainings Sample
        List<Sample> trainingSamples = createSample(metricsList);
        logger.info("Generate Training Sample of size " + trainingSamples.size());


        // Create Filter
        // podName,"namespace","memoryUsageBytes","cpuUsageNanocores","logsfsUsedBytes","rootfsUsedBytes","imageSizeBytes", "image", "containerName","rx_bytes","tx_bytes", "ioServiceRecursiveRead", "ioServiceRecursiveWrite","usedBytesVolume","runningTimeSeconds"
        List<MetricsFilter> filterList = new ArrayList<>();
        filterList.add(new MetricsFilter(null,"all"));
        filterList.add(new MetricsFilter(new boolean[]{true,true,false,false,false,false,false,true,true,false,false,false,false,false,false},"only-strings"));
        filterList.add(new MetricsFilter(new boolean[]{false,false,true,true,true,true,true,false,false,true,true,true,true,true,true}, "only-numbers"));
        filterList.add(new MetricsFilter(new boolean[]{false,false,true,true,true,true,false,false,false,true,true,true,true,true,true}, "only-numbers-no-image-size"));
        String path = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/container-metrics-";

        J48AnomalyDetector j48AnomalyDetector = new J48AnomalyDetector();

        for(MetricsFilter filter:filterList) {

            Instances instancesWithFilter = j48AnomalyDetector.createDatasetWithFilter(trainingSamples, filter.getFilter());
            j48AnomalyDetector.fillDatasetWithFilter(instancesWithFilter, trainingSamples, filter.getFilter());

            // Write to File
            de.lukonjun.metricscollector.helper.FileWriter.writeArffFile(instancesWithFilter,path,filter.getName());

            // Train weka model
            J48 wekaModel = new J48();
            wekaModel.buildClassifier(instancesWithFilter);

            System.out.println("Decision Tree");
            System.out.println(wekaModel.toString());
            DataAggregator.writeToFile(wekaModel.toString(),path + filter.getName() + "-model");
            logger.info("Test our created Model");
            double[] values = new double[]{
                    -2045226423, -1594835516, 8769536, 0, 32768, 65536, 132899597, -1866261913, 104760218, 998, 42, 256, 0, 0, 129744
            };

            // Add another Instance for Testing
            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(instancesWithFilter);
            String label = j48AnomalyDetector.inputInstanceIntoModel(wekaModel, instance);
            assert "nginx".equals(label) : "Label is not the same";
            System.out.println("Nginx Pod get recognized by the model as: " + label);
        }
    }

    private List<Sample> createSample(List<Metrics> metricsList) {
        List<Sample> trainingSamples = new ArrayList<>();
        metricsList.forEach(m -> {
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
        return trainingSamples;
    }

    public static void writeToFile(String str, String filePath) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(str);

        writer.close();
    }

    public List<Metrics> getMetricsTimeInterval(int seconds, List<String> labels) throws Exception {

        List<Metrics> listMetrics = new ArrayList<>();
        List<Metrics> listMetricsDynamic;
        // Get Pod Metrics that dont change over time
        List<Metrics> listMetricsStable = collectStableMetrics(labels);

        for(Metrics m:listMetricsStable){
            listMetricsDynamic = collectDynamicMetrics(seconds,m);
            listMetrics.addAll(listMetricsDynamic);
        }

        // Return List of Stable and Dynamic Metrics
        return listMetrics;
    }

    private List<Metrics> collectDynamicMetrics(int seconds, Metrics m) throws Exception {

        List<Metrics> dynamicMetrics = new ArrayList<>();

        List<Metrics> dockerContainerBlkioMetrics = influxController.getMetricsFromDockerContainerBlkio(seconds,m);
        List<Metrics> kubernetesPodContainerMetrics = influxController.getMetricsFromKubernetesPodContainer(seconds,m);
        List<Metrics> kubernetesPodNetworkMetrics = influxController.getMetricsFromKubernetesPodNetwork(seconds,m);
        List<Metrics> kubernetesPodVolumeMetrics = influxController.getMetricsFromKubernetesPodVolume(seconds,m);

        // get size of smallest list
        int sizeList1 = kubernetesPodNetworkMetrics.size() < kubernetesPodVolumeMetrics.size() ? kubernetesPodNetworkMetrics.size() : kubernetesPodVolumeMetrics.size();
        int sizeList2 = dockerContainerBlkioMetrics.size() < kubernetesPodContainerMetrics.size() ? dockerContainerBlkioMetrics.size() : kubernetesPodContainerMetrics.size();
        int sizeList = sizeList1 < sizeList2 ? sizeList1 : sizeList2;

        for(int i = 0; i < sizeList; i++){
            Metrics blkio = dockerContainerBlkioMetrics.get(i);
            Metrics podMetrics = kubernetesPodContainerMetrics.get(i);
            Metrics podNetwork = kubernetesPodNetworkMetrics.get(i);
            //Metrics2 podVolume = kubernetesPodVolumeMetrics.get(i); TODO Implement Pod Volume
            Metrics newMetricPoint = new Metrics();
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
            logger.debug("blkio time: " + blkio.getTime() + " podMetrics time: " + podMetrics.getTime()+ " podNetwork: " + podNetwork.getTime() + " podVolume: not defined atm");
        }

        return dynamicMetrics;
    }

    private List<Metrics> collectStableMetrics(List<String> labels) throws Exception {
        return podController.collectPodMetrics(labels);
    }

}