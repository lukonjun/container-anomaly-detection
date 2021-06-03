package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.model.Metrics2;
import de.lukonjun.metricscollector.pojo.ContainerImageInfoPojo;
import de.lukonjun.metricscollector.pojo.MetricsPojo;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
@EnableScheduling
public class PodController {

    Logger logger = LoggerFactory.getLogger(PodController.class);

    @Autowired
    ApiConnection apiConnection;

    public List<Metrics2> collectPodMetrics(List<String> labels) throws Exception {

        // Connect to the Kubernetes Api
        ApiConnection.ApiConnectionPojo a = apiConnection.createConnection();

        List<Metrics2> metricsList = fillMetricsObject(a.getClient(), a.getApi(), labels);

        return metricsList;
    }

    private List<Metrics2>fillMetricsObject(ApiClient client, CoreV1Api api, List<String> labels) throws Exception {

        // Define List for Metrics we return later
        List<Metrics2> metricsList = new ArrayList<>();
        String podLabel = null;

        // Container related Information: containerImageNameDigest, containerImageNameTag, containerImageSizeBytes
        List<ContainerImageInfoPojo> imageInfoList = collectContainerImageInfo(api);

        ProtoClient pc = new ProtoClient(client);
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
        for (V1.Pod pod : podList.object.getItemsList()) {
            if ((podLabel = containsLabel(pod, labels)) != null) {
                PodMetricsList podMetricsList = new Metrics(client).getPodMetrics(pod.getMetadata().getNamespace());
                PodMetrics podMetrics = getMatchingPodMetrics(pod,podMetricsList);
                // Pod Metrics will only exist for running Pods, if null continue
                if(podMetrics == null){
                    continue;
                }
                for(ContainerMetrics containerMetrics:podMetrics.getContainers()){
                    if(containerContainsLabel(containerMetrics,labels)){
                        ContainerImageInfoPojo containerInfo = findContainerImageInfo2(imageInfoList, pod, containerMetrics.getName());
                        Metrics2 m = new Metrics2();
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

    private List<Metrics2>fillMetricsPojo(ApiClient client, CoreV1Api api, List<String> labels) throws IOException, ApiException {

        List<ContainerImageInfoPojo> imageInfoList = collectContainerImageInfo(api);

        ProtoClient pc = new ProtoClient(client);
        final ProtoClient.ObjectOrStatus<V1.NamespaceList> namespaceList = pc.list(V1.NamespaceList.newBuilder(), "/api/v1/namespaces");
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");

        List<Metrics2> metrics2List = new ArrayList<>();
        boolean checkForLabels = true;
        String tmpLabel = null;

        // Dont check for specific label
        if(labels == null || labels.isEmpty()){
            checkForLabels = false;
        }

        for(V1.Namespace namespace:namespaceList.object.getItemsList()){
            // Pod Metrics for each namespace
            PodMetricsList podMetricsList = new Metrics(client).getPodMetrics(namespace.getMetadata().getName());

            for(PodMetrics podMetrics:podMetricsList.getItems()){
                V1ObjectMeta metadata = podMetrics.getMetadata();

                for(ContainerMetrics containerMetrics:podMetrics.getContainers()) {
                    boolean matchingLabel = false;
                    if (checkForLabels) {
                        for (String label : labels)
                            if (metadata.getName().contains(label) && containerMetrics.getName().contains(label)) {
                                tmpLabel = label;
                                matchingLabel = true;
                            }
                    }


                    if(matchingLabel || !checkForLabels) {
                        ContainerImageInfoPojo c = findContainerImageInfo(imageInfoList, podList, metadata.getName(), containerMetrics.getName());

                        Map<String, Quantity> stringQuantityMap = containerMetrics.getUsage();
                        Quantity cpuQuantity = stringQuantityMap.get("cpu");
                        Quantity memoryQuantity = stringQuantityMap.get("memory");

                        String podUid = getPodUid(metadata.getName(),metadata.getNamespace(), podList);

                        Metrics2 m = new Metrics2();
                        m.setPodUid(podUid);
                        m.setLabel(tmpLabel);
                        //m.setStartTime(podMetrics.getMetadata().getCreationTimestamp().toInstant());
                        System.out.println(podMetrics.getTimestamp());
                        System.out.println(podMetrics.getMetadata().getCreationTimestamp());
                        System.out.println(m.getStartTime());
                        // StartTime

                        m.setNamespace(metadata.getNamespace()); // Namespace
                        m.setPodName(metadata.getName()); // Pod Name
                        m.setMemoryUsageBytes(memoryQuantity.getNumber().longValue()); // Memory Bytes
                        m.setCpuUsageNanocores(cpuQuantity.getNumber().longValue()); // CPU
                        m.setContainerName(containerMetrics.getName()); // Container Name
                        m.setImageSizeBytes(c.getContainerImageSizeBytes()); // ImageSizeBytes
                        m.setImageName(c.getContainerImageNameDigest()); // ImageNameDigest
                        metrics2List.add(m);
                    }
                }
            }
        }
        //logger.info("Collected Metrics from " + metrics2List.size() + " containers");

        return metrics2List;
    }

    private String getPodUid(String podName, String namespace, ProtoClient.ObjectOrStatus<V1.PodList> podList) {
        for(V1.Pod p:podList.object.getItemsList()) {
                System.out.println(p.getStatus().getStartTime());
                Meta.Time i = p.getStatus().getStartTime();
                if (podName.equals(p.getMetadata().getName()) && namespace.equals(p.getMetadata().getNamespace())) {
                    return p.getMetadata().getUid();
                }
        }
        return null;
    }

    public List<Metrics2> fillMetrics2(ApiClient client, CoreV1Api api) throws IOException, ApiException {

        List<ContainerImageInfoPojo> imageInfoList = collectContainerImageInfo(api);

        ProtoClient pc = new ProtoClient(client);
        final ProtoClient.ObjectOrStatus<V1.NamespaceList> namespaceList = pc.list(V1.NamespaceList.newBuilder(), "/api/v1/namespaces");
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
        List<Metrics2> metrics2List = new ArrayList<>();

        for(V1.Namespace namespace:namespaceList.object.getItemsList()){
            // Pod Metrics for each namespace
            PodMetricsList podMetricsList = new Metrics(client).getPodMetrics(namespace.getMetadata().getName());

            for(PodMetrics podMetrics:podMetricsList.getItems()){
                V1ObjectMeta metadata = podMetrics.getMetadata();

                for(ContainerMetrics containerMetrics:podMetrics.getContainers()){

                    ContainerImageInfoPojo c = findContainerImageInfo(imageInfoList, podList, metadata.getName(), containerMetrics.getName());

                    Metrics2 m = new Metrics2();
                    m.setNamespace(podMetrics.getMetadata().getNamespace());
                    //m.setStartTime(podMetrics.getMetadata().getCreationTimestamp().toInstant());
                    m.setPodName(metadata.getName());
                    m.setContainerName(containerMetrics.getName());
                    m.setImageName(c.getContainerImageNameDigest()); // ImageNameDigest
                    m.setImageSizeBytes(c.getContainerImageSizeBytes()); // ImageSizeBytes
                    metrics2List.add(m);
                }
            }
        }
        //logger.info("Collected Metrics from " + metrics2List.size() + " containers");

        return metrics2List;
    }


    /**
     * Find the Container Info that matches to the Container
     * @param imageInfoList
     * @param podList
     * @param podName
     * @param containerName
     * @return
     */
    private ContainerImageInfoPojo findContainerImageInfo(List<ContainerImageInfoPojo> imageInfoList, ProtoClient.ObjectOrStatus<V1.PodList> podList, String podName, String containerName) {

        String image = null;
        boolean check = false;
        for(V1.Pod p:podList.object.getItemsList()) {
            for (V1.Container c : p.getSpec().getContainersList()) {
                if (podName.equals(p.getMetadata().getName()) && containerName.equals(c.getName())) {
                    image = c.getImage();
                    check = true;
                }
            }
        }
        if(!check){
            logger.error("Found no matching Image for podName: " + podName + " containerName: " + containerName);
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

        logger.error("Did not find Container Image Information for the Container" + containerName+ " in Pod " +podName );
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
