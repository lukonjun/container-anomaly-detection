package de.lukonjun.podwatcher.controller;

import de.lukonjun.podwatcher.data.DataAggregator;
import de.lukonjun.podwatcher.kubernetes.ApiConnection;
import de.lukonjun.podwatcher.ml.J48AnomalyDetector;
import de.lukonjun.podwatcher.ml.LoadModel;
import de.lukonjun.podwatcher.ml.Sample;
import de.lukonjun.podwatcher.model.Metrics;
import de.lukonjun.podwatcher.pojo.ContainerImageInfoPojo;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.proto.V1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Controller
@EnableScheduling
public class PodController {

    private final static List<String> POD_LIST = new ArrayList<>();

    private static boolean startUp = true;

    Logger logger = LoggerFactory.getLogger(PodController.class);

    @Value("#{'${data.aggregator.decision.tree.classifier.list}'.split(',')}")
    private List<String> labels;

    @Value("${timeout.fetching.metrics}")
    private int timeOutFetchingMetricsMilliseconds;

    @Autowired
    ApiConnection apiConnection;

    @Autowired
    LoadModel loadModel;

    @Autowired
    DataAggregator dataAggregator;

    // Example from https://github.com/kubernetes-client/java/blob/master/examples/examples-release-12/src/main/java/io/kubernetes/client/examples/KubeConfigFileClientExample.java
    @Scheduled(fixedRateString = "${pod.controller.watch.rate:5000}")
    private void watchPodsSpawn() throws Exception {

        // Configuration Time Interval Metrics
        int timeIntervalSeconds = 120;
        int timeOutFetchingMetricsMilliseconds = 120000;

        V1PodList list =
                apiConnection.createConnection().getApi().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        logger.info("Start watching pods");
        // check for running Pods
        for (V1Pod item : list.getItems()) {
            String podName = item.getMetadata().getName();
            if(!POD_LIST.contains(podName)){
                POD_LIST.add(podName);
                System.out.println("Recognized Pod " + podName);
                // Implementation with Runnable
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        long threadStartRunningTime = System.currentTimeMillis();
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + " running");

                        List<Metrics> metricsList = null;
                        ArrayList<String> podNameList = new ArrayList<>();
                        podNameList.add(podName);
                        // time Problem, run only for certain seconds amount
                        long startTime = System.currentTimeMillis();
                        // Try for 60 seconds
                        while((metricsList == null || metricsList.size() == 0)&&(System.currentTimeMillis()-startTime)<timeOutFetchingMetricsMilliseconds){
                            try {
                                metricsList = dataAggregator.getMetricsTimeInterval(timeIntervalSeconds,podNameList, true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //System.out.println((System.currentTimeMillis()-startTime)/1000);
                        }
                        if(metricsList == null || metricsList.size() == 0){
                            System.out.println("Timeout for " + podName + " cant classify as we cannot fetch Metrics in the Time Interval of " + timeOutFetchingMetricsMilliseconds+ " Milliseconds");
                        }else {
                            List<Metrics> metricsList1 = new ArrayList<>();
                            metricsList1.add(metricsList.get(0));
                            List<Sample> trainingSamples = dataAggregator.createSample(metricsList1);

                            J48AnomalyDetector j48AnomalyDetector = new J48AnomalyDetector(labels);
                            // Set Up Instances
                            // Instances instancesWithFilter = j48AnomalyDetector.createDatasetWithFilter(trainingSamples, null);
                            // j48AnomalyDetector.fillDatasetWithFilter(instancesWithFilter, trainingSamples, null);

                            // Validate against Model
                            boolean[] filterArray = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true, true, true, true};
                            // Need to add Serialized Model here loadModel.getWekaModel()
                            String modelResult = null;
                            try {
                                modelResult = j48AnomalyDetector.validateModel(loadModel.getWekaModel(), trainingSamples.get(0), filterArray, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println(podName + " got classified by the model as " + modelResult);

                            System.out.println(threadName + " finished");
                        }
                        long totalThreadRunningTime = System.currentTimeMillis()-threadStartRunningTime;
                        System.out.println("-- fetch time in milliseconds " + podName + " " + totalThreadRunningTime);

                    }
                };

                Thread threadRunnableInterface = new Thread( runnable, "Thread " + podName);
                threadRunnableInterface.start();
            }
        }
        PodController.startUp = false;

        // check for deleted pods
        List<String> found = new ArrayList<String>();
        for(String podName:POD_LIST){
            if(!containsPod(podName,list)){
                found.add(podName);
            }
        }

        // delete pods
        for(String podName:found) {
            System.out.println("Pod " + podName + " is deleted");
            POD_LIST.remove(podName);
        }
    }

    /**
     * Checks if a V1PodList contains a Pod with podName
     * @param podName
     * @param list
     * @return
     */
    private boolean containsPod(String podName, V1PodList list){
        ArrayList<String> listPodName = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPodName.add(item.getMetadata().getName());
        }
        return listPodName.contains(podName);
    }

    public List<Metrics> collectPodMetrics(List<String> labels, boolean ignoreLabelForContainers) throws Exception {

        // Connect to the Kubernetes Api
        ApiConnection.ApiConnectionPojo a = apiConnection.createConnection();

        List<Metrics> metricsList = fillMetricsObject(a.getClient(), a.getApi(), labels, ignoreLabelForContainers);

        return metricsList;
    }

    private List<Metrics>fillMetricsObject(ApiClient client, CoreV1Api api, List<String> labels, boolean ignoreLabelForContainers) throws Exception {

        // Define List for Metrics we return later
        List<Metrics> metricsList = new ArrayList<>();
        String podLabel = null;

        // Container related Information: containerImageNameDigest, containerImageNameTag, containerImageSizeBytes
        List<ContainerImageInfoPojo> imageInfoList = collectContainerImageInfo(api);

        ProtoClient pc = new ProtoClient(client);
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
        for (V1.Pod pod : podList.object.getItemsList()) {
            if ((podLabel = containsLabel(pod, labels)) != null) {
                PodMetricsList podMetricsList = new io.kubernetes.client.Metrics(client).getPodMetrics(pod.getMetadata().getNamespace());
                PodMetrics podMetrics = getMatchingPodMetrics(pod,podMetricsList);
                // Pod Metrics will only exist for running Pods, if null continue
                if(podMetrics == null){
                    continue;
                }
                for(ContainerMetrics containerMetrics:podMetrics.getContainers()){
                    if(containerContainsLabel(containerMetrics,labels )|| ignoreLabelForContainers){ // Problem maybe here, matching between name and container ????
                        ContainerImageInfoPojo containerInfo = findContainerImageInfo2(imageInfoList, pod, containerMetrics.getName());
                        Metrics m = new Metrics();
                        m.setLabel(podLabel);
                        m.setPodName(pod.getMetadata().getName());
                        m.setNamespace(pod.getMetadata().getNamespace());
                        m.setPodUid(pod.getMetadata().getUid());
                        m.setCpuUsageNanocores(containerMetrics.getUsage().get("cpu").getNumber().longValue());
                        m.setMemoryUsageBytes(containerMetrics.getUsage().get("memory").getNumber().longValue());
                        m.setContainerName(containerMetrics.getName());
                        m.setStartTime(pod.getStatus().getStartTime());
                        m.setImageSizeBytes(containerInfo.getContainerImageSizeBytes()); // ImageSizeBytes
                        m.setImageName(containerInfo.getContainerImageNameDigest()); // ImageNameDigest
                        metricsList.add(m);
                    }
                }
            }
        }

        logger.debug(this.getClass().getName() + " Collected Metrics total " + metricsList.size());
        return metricsList;
    }

    private ContainerImageInfoPojo findContainerImageInfo2(List<ContainerImageInfoPojo> imageInfoList, V1.Pod pod, String containerName) throws Exception {

        String image = null;
        boolean check = false;
        for (V1.Container c : pod.getSpec().getContainersList()) {
            if (containerName.equals(c.getName())) {
                    image = c.getImage();
                    check = true;
                    break;
            }
        }

        if(!check){
            throw new Exception("Found no Image for podName: " + pod.getMetadata().getName() + " containerName: " + containerName);
        }

        image = image.substring(image.indexOf("/")+1);
        image.trim();
        for(ContainerImageInfoPojo c:imageInfoList){
            if(c.getContainerImageNameDigest() != null && c.getContainerImageNameDigest().contains(image)){
                return c;
            }
            // Problem is docker.io/bitnami/mysql:8.0.24-debian-10-r0 image string, cache bitnami/mysql:8.0.24-debian-10-r0
            if(c.getContainerImageNameTag() != null && c.getContainerImageNameTag().contains(image)){
                return c;
            }
        }

        logger.error("Did not find Container Image Information for the Container" + containerName+ " in Pod " + pod.getMetadata().getName());
        return null;
    }

    private boolean containerContainsLabel(ContainerMetrics containerMetrics, List<String> labels) {
        for (String label : labels) {
            if (containerMetrics.getName().contains(label)) {
                return true;
            }
        }
        return false;
    }

    private PodMetrics getMatchingPodMetrics(V1.Pod pod, PodMetricsList podMetricsList) throws Exception {
        for(PodMetrics podMetrics:podMetricsList.getItems()){
            if(podMetrics.getMetadata().getName().equals(pod.getMetadata().getName())){
                return podMetrics;
            }
        }
        return null;
    }

    private String containsLabel(V1.Pod pod, List<String> labels) {
        for (String label : labels) {
            if (pod.getMetadata().getName().contains(label)) {
                return label;
            }
        }
        return null;
    }

    public List<ContainerImageInfoPojo> collectContainerImageInfo(CoreV1Api api) throws ApiException {
        List<ContainerImageInfoPojo> imageInfoList = new ArrayList<>();

        // List Nodes
        V1NodeList list = api.listNode(null, null, null, null,null,null, null, null, null, null);
        for (V1Node item : list.getItems()) {

            V1NodeStatus nodeStatus = item.getStatus();
            List<V1ContainerImage> imageList = nodeStatus.getImages();
            for(V1ContainerImage containerImage:imageList){

                List<String> listContainerNames = containerImage.getNames();

                String containerImageNameDigest = listContainerNames.get(0);
                String containerImageNameTag = null;
                if(listContainerNames.size() >= 2) {
                    containerImageNameTag = listContainerNames.get(1);
                }
                if(containsImage(imageInfoList, listContainerNames.get(0)))continue;

                ContainerImageInfoPojo c = new ContainerImageInfoPojo();
                c.setContainerImageSizeBytes(containerImage.getSizeBytes());
                c.setContainerImageNameDigest(containerImageNameDigest);
                c.setContainerImageNameTag(containerImageNameTag);
                imageInfoList.add(c);
            }
        }

        return imageInfoList;
    }

    private boolean containsImage(List<ContainerImageInfoPojo> imageInfoList, String containerImageNameDigest){
        for(ContainerImageInfoPojo c:imageInfoList){
            if(c.getContainerImageNameDigest().equals(containerImageNameDigest)) return true;
        }
        return false;
    }

}
