package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.model.ModelPodMetrics;
import de.lukonjun.metricscollector.repository.PodRepository;
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
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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
    PodRepository podRepository;

    @Scheduled(fixedRateString = "${pod.controller.metrics.collection.rate}")
    private void metricsCollectionWriteToDatabase() throws IOException, ApiException {
        List<ModelPodMetrics> modelPodMetricsList = collectMetricsFromAllPods();
        if(writeMetricsToDatabase(modelPodMetricsList)){
            logger.info("Successfully wrote " + modelPodMetricsList.size() + " to database");
        }else{
            logger.error("Something went wrong");
        }
    }

    //TODO Best practise for return values here
    private boolean writeMetricsToDatabase(List<ModelPodMetrics> modelPodMetricsList){
        List<ModelPodMetrics> returnValue = podRepository.saveAll(modelPodMetricsList);
        if(!returnValue.isEmpty())return true;
        else return false;
    }

    private List<ModelPodMetrics> collectMetricsFromAllPods() throws IOException, ApiException {

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

        // get a list of all namespaces
        ProtoClient pc = new ProtoClient(client);
        ProtoClient.ObjectOrStatus<V1.NamespaceList> list = pc.list(V1.NamespaceList.newBuilder(), "/api/v1/namespaces");

        List<ModelPodMetrics> modelPodMetricsList = new ArrayList<>();

        for(V1.Namespace namespace:list.object.getItemsList()){
            // Get pod metrics for each namespace per iteration
            PodMetricsList podMetricsList = new Metrics(client).getPodMetrics(namespace.getMetadata().getName());

            for(PodMetrics podMetrics:podMetricsList.getItems()){
                V1ObjectMeta metadata = podMetrics.getMetadata();
                ModelPodMetrics m = new ModelPodMetrics();
                // Namespace
                m.setNamespace(metadata.getNamespace());
                // Pod Name
                m.setPodName(metadata.getName());
                for(ContainerMetrics containerMetrics:podMetrics.getContainers()){
                    Map<String, Quantity> stringQuantityMap = containerMetrics.getUsage();
                    Quantity cpuQuantity = stringQuantityMap.get("cpu");
                    Quantity memoryQuantity = stringQuantityMap.get("memory");
                    // Container Name
                    m.setContainerName(containerMetrics.getName());
                    // Memory
                    m.setMemory(memoryQuantity.getNumber().intValue());
                    // CPU
                    m.setCpu(cpuQuantity.getNumber().floatValue());
                    modelPodMetricsList.add(m);
                }
            }
        }
        logger.info("Collected Metrics from " +modelPodMetricsList.size() + " containers");
        return modelPodMetricsList;
    }

}
