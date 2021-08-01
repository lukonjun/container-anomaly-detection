package de.lukonjun.metricscollector.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;

@Component
public class ApiConnection {

    Logger logger = LoggerFactory.getLogger(ApiConnection.class);

    @Value("${kubernetes.connect.from.outside}")
    private boolean connectFromOutside;

    public ApiConnectionPojo createConnection() throws IOException {
        ApiConnectionPojo apiConnectionPojo = new ApiConnectionPojo();
        if(connectFromOutside){

            logger.debug("Connect to the Kubernetes Api from outside of the Cluster");

            // file path to your KubeConfig
            String kubeConfigPath = System.getenv("HOME") + "/.kube/config";

            // loading the out-of-cluster config, a kubeconfig from file-system
            ApiClient client =
                    ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();

            // set the global default api-client to the in-cluster one from above
            Configuration.setDefaultApiClient(client);

            // the CoreV1Api loads default api-client from global configuration.
            CoreV1Api api = new CoreV1Api();

            apiConnectionPojo.setApi(api);
            apiConnectionPojo.setClient(client);
        }else{

            logger.debug("Connect to the Kubernetes Api from the inside of the Cluster");

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

            apiConnectionPojo.setApi(api);
            apiConnectionPojo.setClient(client);
        }
        return apiConnectionPojo;
    }

    public class ApiConnectionPojo{
        CoreV1Api api;
        ApiClient client;

        public void setApi(CoreV1Api api) {
            this.api = api;
        }

        public void setClient(ApiClient client) {
            this.client = client;
        }

        public CoreV1Api getApi() {
            return api;
        }

        public ApiClient getClient() {
            return client;
        }
    }

}
