package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.Pojo.ContainerImageInfo;
import de.lukonjun.metricscollector.Pojo.MetricsPojo;
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
/*
        // file path to your KubeConfig
        String kubeConfigPath = System.getenv("HOME") + "/.kube/config";

        // loading the out-of-cluster config, a kubeconfig from file-system
        ApiClient client =
                ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();

        // set the global default api-client to the in-cluster one from above
        Configuration.setDefaultApiClient(client);
        /
        // the CoreV1Api loads default api-client from global configuration.
        CoreV1Api api = new CoreV1Api();
*/
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


        List<MetricsPojo> metricsPojoList = fillMetricsPojo(client, api);

        return metricsPojoList;
    }

    private List<MetricsPojo> fillMetricsPojo(ApiClient client, CoreV1Api api) throws IOException, ApiException {

        List<ContainerImageInfo> imageInfoList = collectContainerImageInfo(api);

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

                    ContainerImageInfo c = findContainerImageInfo(imageInfoList, podList, metadata.getName(), containerMetrics.getName());

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

    /**
     * Find the Container Info that matches to the Container
     * @param imageInfoList
     * @param podList
     * @param podName
     * @param containerName
     * @return
     */
    private ContainerImageInfo findContainerImageInfo(List<ContainerImageInfo> imageInfoList, ProtoClient.ObjectOrStatus<V1.PodList> podList, String podName, String containerName) {

        String image = null;

        for(V1.Pod p:podList.object.getItemsList()) {
            for (V1.Container c : p.getSpec().getContainersList()) {
                if (podName.equals(p.getMetadata().getName()) && containerName.equals(c.getName())) {
                    image = c.getImage();
                }
            }
        }
        for(ContainerImageInfo c:imageInfoList){
            if(c.getContainerImageNameTag().contains(image)){
                return c;
            }
        }

        logger.error("Did not find Container Image Information for the Container" + containerName+ " in Pod " +podName );
        return null;
    }

    public List<ContainerImageInfo> collectContainerImageInfo(CoreV1Api api) throws ApiException {
        List<ContainerImageInfo> imageInfoList = new ArrayList<>();

        // List Nodes
        V1NodeList list = api.listNode(null, null, null, null,null,null, null, null, null, null);
        for (V1Node item : list.getItems()) {

            V1NodeStatus nodeStatus = item.getStatus();
            List<V1ContainerImage> imageList = nodeStatus.getImages();
            for(V1ContainerImage containerImage:imageList){

                List<String> listContainerNames = containerImage.getNames();
                if(listContainerNames.size() != 2){
                    logger.error("listContainerNames is of size " + listContainerNames.size() + " even though it should be always of size 2");
                }

                String containerImageNameDigest = listContainerNames.get(0);
                String containerImageNameTag = listContainerNames.get(1);

                if(containsImage(imageInfoList, listContainerNames.get(0)))continue;

                ContainerImageInfo c = new ContainerImageInfo();
                c.setContainerImageSizeBytes(containerImage.getSizeBytes());
                c.setContainerImageNameDigest(containerImageNameDigest);
                c.setContainerImageNameTag(containerImageNameTag);
                imageInfoList.add(c);
            }
        }

        return imageInfoList;
    }

    private boolean containsImage(List<ContainerImageInfo> imageInfoList,String containerImageNameDigest){
        for(ContainerImageInfo c:imageInfoList){
            if(c.getContainerImageNameDigest().equals(containerImageNameDigest)) return true;
        }
        return false;
    }

}
