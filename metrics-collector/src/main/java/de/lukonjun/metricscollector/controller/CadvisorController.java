package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.MetricsCollectorApplication;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CadvisorController {

    @Value("${kubernetes.api.endpoint:http://kubernetes.default.svc}")
    private String apiString;

    @Scheduled(fixedRateString = "5000")
    public void getCadvisorMetrics() throws ApiException, IOException {

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
        List<String> nodeNameList = new ArrayList<>();
        V1NodeList list = api.listNode(null, null, null, null,null,null, null, null, null, null);
        for (V1Node item : list.getItems()) {
            nodeNameList.add(item.getMetadata().getName());
        }
        // parse prometheus values
        // https://stackoverflow.com/questions/53971589/instrumenting-prometheus-metrics-with-java-client
        // http://localhost:8001/api/v1/nodes/worker01/proxy/metrics/cadvisor
        String kubernetesApi =  this.apiString;
        //kubernetesApi = "http://localhost:8001";

        for(String nodeName:nodeNameList) {
            String url = kubernetesApi + "/api/v1/nodes/" + nodeName + "/proxy/metrics/cadvisor";
            System.out.println(url);
            String s = urlReader(url);
            System.out.println("got response of size" + s.getBytes() + " bytes");
        }

    }



    // http://zetcode.com/java/readwebpage/
    private String urlReader(String sourceUrl) throws IOException {

        HttpGet request = null;
        String content = null;

        try {
            String url = sourceUrl;

            int timeout = 5;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000).build();
            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            request = new HttpGet(url);
            request.addHeader("Content-Type","text/plain;charset=UTF-8");
            request.addHeader("User-Agent", "Apache HTTPClient");
            HttpResponse response = client.execute(request);

            HttpEntity entity = response.getEntity();
            content = EntityUtils.toString(entity, "UTF-8");

        }catch (Exception e) {
            e.printStackTrace(System.out);
        }
        finally {

            if (request != null) {

                request.releaseConnection();
            }
        }

        return content;
    }
}
