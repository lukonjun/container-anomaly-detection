package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import de.lukonjun.metricscollector.model.Metrics;
import de.lukonjun.metricscollector.pojo.ContainerImageInfoPojo;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.proto.V1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;

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

    public List<Metrics> collectPodMetrics(List<String> labels) throws Exception {

        // Connect to the Kubernetes Api
        ApiConnection.ApiConnectionPojo a = apiConnection.createConnection();

        List<Metrics> metricsList = fillMetricsObject(a.getClient(), a.getApi(), labels);

        return metricsList;
    }

    private List<Metrics>fillMetricsObject(ApiClient client, CoreV1Api api, List<String> labels) throws Exception {

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
                    if(containerContainsLabel(containerMetrics,labels)){
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
