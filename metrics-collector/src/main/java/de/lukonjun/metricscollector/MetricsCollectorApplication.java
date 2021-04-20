package de.lukonjun.metricscollector;

import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import de.lukonjun.metricscollector.controller.PodController;
import de.lukonjun.metricscollector.model.ModelPodMetrics;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class MetricsCollectorApplication {

	Logger logger = LoggerFactory.getLogger(MetricsCollectorApplication.class);

	public static void main(String[] args) throws IOException, ApiException {
		SpringApplication.run(MetricsCollectorApplication.class, args);
	}

	public void getImageSize() throws ApiException, IOException {

		// file path to your KubeConfig
		String kubeConfigPath = System.getenv("HOME") + "/.kube/config";

		// loading the out-of-cluster config, a kubeconfig from file-system
		ApiClient client =
				ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();

		// set the global default api-client to the in-cluster one from above
		Configuration.setDefaultApiClient(client);

		// the CoreV1Api loads default api-client from global configuration.
		CoreV1Api api = new CoreV1Api();

		// List Nodes
		V1NodeList list = api.listNode(null, null, null, null,null,null, null, null, null, null);
		for (V1Node item : list.getItems()) {
			System.out.println(item.getMetadata().getName());
			V1NodeStatus nodeStatus = item.getStatus();
			List<V1ContainerImage> imageList = nodeStatus.getImages();
			for(V1ContainerImage containerImage:imageList){
				// Take both names? Maybe the second part needed for the Mapping
				// Save in a Map?
				System.out.println(containerImage.getNames());
				System.out.println(containerImage.getSizeBytes());
			}
		}

	}

}
