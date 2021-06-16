package de.lukonjun.podwatcher.controller;

import de.lukonjun.podwatcher.kubernetes.ApiConnection;
import de.lukonjun.podwatcher.ml.LoadModel;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@EnableScheduling
public class PodController {

    private final static List<String> POD_LIST = new ArrayList<>();

    Logger logger = LoggerFactory.getLogger(PodController.class);

    @Autowired
    ApiConnection apiConnection;

    @Autowired
    LoadModel loadModel;

    // Example from https://github.com/kubernetes-client/java/blob/master/examples/examples-release-12/src/main/java/io/kubernetes/client/examples/KubeConfigFileClientExample.java
    @Scheduled(fixedRateString = "${pod.controller.scheduling.rate:5000}")
    private void watchPodsSpawn() throws IOException, ApiException {

        V1PodList list =
                apiConnection.createConnection().getApi().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        logger.info("watching pods");
        // check for running Pods
        for (V1Pod item : list.getItems()) {
            String podName = item.getMetadata().getName();
            if(!POD_LIST.contains(podName)){
                POD_LIST.add(podName);
                System.out.println("Pod " + podName + " spawned");
                List<>
                // Fetch Metrics for Containers in this Pod
                // Classify both Containers
                // Thats it
            }
        }

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

}
