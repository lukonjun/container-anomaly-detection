package de.lukonjun.metricscollector.controller;

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
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        public List<MetricsPojo> collectPodMetrics() throws IOException, ApiException {

        // file path to your KubeConfig
        String kubeConfigPath = System.getenv("HOME") + "/.kube/config";

        // loading the out-of-cluster config, a kubeconfig from file-system
        ApiClient client =
                ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();

        // set the global default api-client to the in-cluster one from above
        Configuration.setDefaultApiClient(client);

        // the CoreV1Api loads default api-client from global configuration.
        CoreV1Api api = new CoreV1Api();
/*
        // loading the in-cluster config, including:
        //   1. service-account CA
        //   2. service-account bearer-token
        //   3. service-account namespace
        //   4. master endpoints(ip, port) from pre-set environment variables
        ApiClient client = ClientBuilder.cluster().build();

        // if you prefer not to refresh service account token, please use:
        // ApiClient client = ClientBuilder.oldCluster().build();

        // set the global default api-client to the in-cluster one from above
        Configuration.setDefaultApiClient(client);

        // the CoreV1Api loads default api-client from global configuration.
        CoreV1Api api = new CoreV1Api();
*/

        List<MetricsPojo> metricsPojoList = fillMetricsPojo(client, api);

        return metricsPojoList;
    }



    private List<MetricsPojo> fillMetricsPojo(ApiClient client, CoreV1Api api) throws IOException, ApiException {

        List<ContainerImageInfoPojo> imageInfoList = collectContainerImageInfo(api);

        ProtoClient pc = new ProtoClient(client);
        final ProtoClient.ObjectOrStatus<V1.NamespaceList> namespaceList = pc.list(V1.NamespaceList.newBuilder(), "/api/v1/namespaces");
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
        List<MetricsPojo> metricsPojoList = new ArrayList<>();

        for(V1.Namespace namespace:namespaceList.object.getItemsList()){
            // Pod Metrics for each namespace
            PodMetricsList podMetricsList = new Metrics(client).getPodMetrics(namespace.getMetadata().getName());

            for(PodMetrics podMetrics:podMetricsList.getItems()){
                V1ObjectMeta metadata = podMetrics.getMetadata();

                for(ContainerMetrics containerMetrics:podMetrics.getContainers()){

                    ContainerImageInfoPojo c = findContainerImageInfo(imageInfoList, podList, metadata.getName(), containerMetrics.getName());

                    Map<String, Quantity> stringQuantityMap = containerMetrics.getUsage();
                    Quantity cpuQuantity = stringQuantityMap.get("cpu");
                    Quantity memoryQuantity = stringQuantityMap.get("memory");

                    MetricsPojo m = new MetricsPojo();
                    m.setNamespace(metadata.getNamespace()); // Namespace
                    m.setPodName(metadata.getName()); // Pod Name
                    m.setContainerName(containerMetrics.getName());
                    m.setMemoryBytes(memoryQuantity.getNumber().longValue()); // Memory Bytes
                    m.setCpu(cpuQuantity.getNumber().doubleValue()); // CPU
                    m.setImage(c.getContainerImageNameDigest()); // ImageNameDigest
                    m.setImageSizeBytes(c.getContainerImageSizeBytes()); // ImageSizeBytes
                    metricsPojoList.add(m);
                }
            }
        }
        logger.info("Collected Metrics from " + metricsPojoList.size() + " containers");

        return metricsPojoList;
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
                    m.setPodName(metadata.getName());
                    m.setContainerName(containerMetrics.getName());
                    m.setImage(c.getContainerImageNameDigest()); // ImageNameDigest
                    m.setImageSizeBytes(c.getContainerImageSizeBytes()); // ImageSizeBytes
                    metrics2List.add(m);
                }
            }
        }
        logger.info("Collected Metrics from " + metrics2List.size() + " containers");

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
