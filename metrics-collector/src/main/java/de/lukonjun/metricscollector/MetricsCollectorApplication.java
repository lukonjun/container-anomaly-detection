package de.lukonjun.metricscollector;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;

@SpringBootApplication
public class MetricsCollectorApplication {

	Logger logger = LoggerFactory.getLogger(MetricsCollectorApplication.class);

	public static void main(String[] args) throws IOException, ApiException {
		SpringApplication.run(MetricsCollectorApplication.class, args);
	}
}
