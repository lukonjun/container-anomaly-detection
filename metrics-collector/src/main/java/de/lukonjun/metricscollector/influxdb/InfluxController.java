package de.lukonjun.metricscollector.influxdb;

import de.lukonjun.metricscollector.Pojo.MetricsPojo;
import de.lukonjun.metricscollector.controller.PodController;
import io.kubernetes.client.openapi.ApiException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class InfluxController {

    Logger logger = LoggerFactory.getLogger(PodController.class);

    @Value("${influxdb.server.url}")
    private String serverUrl;

    @Value("${influxdb.username}")
    private String userName;

    @Value("${influxdb.password}")
    private String password;

    @Autowired
    PodController podController;
    /*
        Code ExamplesDocumentation
        https://www.baeldung.com/java-influxdb (Query Data)
        https://github.com/influxdata/influxdb-java
        Documentation
        https://devconnected.com/the-definitive-guide-to-influxdb-in-2019/ (Full Guide)
     */
    @Scheduled(fixedRateString = "${influxdb.metrics.collection.rate:10000}")
    public void connectToDatabase() throws InterruptedException, IOException, ApiException {

        InfluxDB influxDB = InfluxDBFactory.connect(serverUrl, userName, password);

        String databaseName = "pod_metrics";
        String retentionPolicyName = "retention_pod_metrics";
        influxDB.query(new Query("CREATE DATABASE " + databaseName));
        influxDB.setDatabase(databaseName);

        influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
                + " ON " + databaseName + " DURATION 30d REPLICATION 1 DEFAULT"));
        influxDB.setRetentionPolicy(retentionPolicyName);

        List<MetricsPojo> metricsPojoList = podController.collectPodMetrics();

        BatchPoints batchPoints = BatchPoints
                .database(databaseName)
                .retentionPolicy(retentionPolicyName)
                .build();

        for(MetricsPojo mp:metricsPojoList){
            Point point = Point.measurement("metrics")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .tag("pod_name", mp.getPodName())
                    .tag("namespace", mp.getNamespace())
                    .addField("imageSizeBytes", mp.getImageSizeBytes())
                    .tag("image", mp.getImage())
                    .addField("cpu", mp.getCpu())
                    .addField("memoryBytes", mp.getMemoryBytes())
                    .build();

            batchPoints.point(point);
        }

        influxDB.write(batchPoints);

        influxDB.close();
    }

}
