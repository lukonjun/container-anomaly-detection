package de.lukonjun.metricscollector.influxdb;

import de.lukonjun.metricscollector.model.*;
import de.lukonjun.metricscollector.pojo.ContainerPojo;
import de.lukonjun.metricscollector.pojo.MetricsPojo;
import de.lukonjun.metricscollector.controller.PodController;
import io.kubernetes.client.openapi.ApiException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.influxdb.impl.InfluxDBResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@PropertySource("classpath:application.properties")
public class InfluxController {

    Logger logger = LoggerFactory.getLogger(PodController.class);

    @Value("${influxdb.server.url}")
    private String serverUrl;

    @Value("${influxdb.username}")
    private String userName;

    @Value("${influxdb.password}")
    private String password;

    @Value("${influxdb.database}")
    private String database;

    private InfluxDB connection;

    public InfluxDB getConnection() {
        return connection;
    }

    public void setConnection(InfluxDB connection) {
        this.connection = connection;
    }

    // https://www.baeldung.com/spring-value-annotation
    public InfluxController(@Value("${influxdb.server.url:default}") String serverUrl, @Value("${influxdb.username:default}") String userName, @Value("${influxdb.password:default}") String password,  @Value("${influxdb.database:default}") String database){
        this.connection = InfluxDBFactory.connect(serverUrl, userName, password);
        this.connection.setDatabase(database);
    }

    @Autowired
    PodController podController;
    /*
        Code ExamplesDocumentation
        https://www.baeldung.com/java-influxdb (Query Data)
        https://github.com/influxdata/influxdb-java
        Documentation
        https://devconnected.com/the-definitive-guide-to-influxdb-in-2019/ (Full Guide)
     */
    //@Scheduled(fixedRateString = "${influxdb.metrics.collection.rate:10000}")
    public void getMetricsFromInflux() throws InterruptedException, IOException, ApiException {

        InfluxDB influxDB = InfluxDBFactory.connect(serverUrl, userName, password);

        String databaseName = "pod_metrics";
        String retentionPolicyName = "retention_pod_metrics";
        influxDB.query(new Query("CREATE DATABASE " + databaseName));
        influxDB.setDatabase(databaseName);

        influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
                + " ON " + databaseName + " DURATION 30d REPLICATION 1 DEFAULT"));
        influxDB.setRetentionPolicy(retentionPolicyName);

        List<MetricsPojo> metricsPojoList = null;
                //podController.collectPodMetrics();

        boolean printOutMetrics = true;
        if(printOutMetrics) {
            metricsPojoList.forEach(mp -> System.out.println(mp));
        }
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

    //@Scheduled(fixedRateString = "${influxdb.metrics.collection.rate:10000}")
    public void connectToDatabase2() throws InterruptedException, IOException, ApiException {

        InfluxDB connection = InfluxDBFactory.connect(serverUrl, userName, password);
        String databaseName = "k3s_telegraf_ds";
        String retentionPolicyName = "retention_pod_metrics";
        connection.setDatabase(databaseName);
        connection.setRetentionPolicy(retentionPolicyName);

        // Telegraf Data gets fetched every 10 Seconds
        // Could also check Results of last 5 Minutes, idk
        // What Time Interval do we check?
        QueryResult queryResult = connection
                .query(new Query("Select * from kubernetes_pod_container LIMIT 10"));

        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<KubernetesPodContainer> memoryPointList = resultMapper
                .toPOJO(queryResult, KubernetesPodContainer.class);

        memoryPointList.forEach(mp -> System.out.println(mp));

        connection.close();
    }

    public List<KubernetesPodContainer> selectFromKubernetesPodContainer(String query){
        QueryResult queryResult = connection
                .query(new Query(query));

        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<KubernetesPodContainer> memoryPointList = resultMapper
                .toPOJO(queryResult, KubernetesPodContainer.class);

        return memoryPointList;
    }

    public List<KubernetesPodNetwork> selectFromKubernetesPodNetwork(String query){
        QueryResult queryResult = connection
                .query(new Query(query));

        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<KubernetesPodNetwork> memoryPointList = resultMapper
                .toPOJO(queryResult, KubernetesPodNetwork.class);

        return memoryPointList;
    }

    public List<KubernetesPodVolume> selectFromKubernetesPodNVolume(String query){
        QueryResult queryResult = connection
                .query(new Query(query));

        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<KubernetesPodVolume> memoryPointList = resultMapper
                .toPOJO(queryResult, KubernetesPodVolume.class);

        return memoryPointList;
    }

    public List<DockerContainerBlkio> selectDockerContainerBlkio(String query){
        QueryResult queryResult = connection
                .query(new Query(query));

        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<DockerContainerBlkio> memoryPointList = resultMapper
                .toPOJO(queryResult, DockerContainerBlkio.class);

        return memoryPointList;
    }

    public List<Metrics2> getMetricsFromDockerContainerBlkio(int seconds, List<String> labels, Set<String> setPodUids) {
        String query = "SELECT * FROM \"docker_container_blkio\" WHERE time > now() -" + seconds + "s";
        // container id, container_image, container_name
        // io.kubernetes.container.name io.kubernetes.pod.name io.kubernetes.pod.namespace io.kubernetes.pod.uid
        List<Metrics2> metrics2List = new ArrayList<>();
        /*
        for(String podUid)
        List<DockerContainerBlkio> blkioList = selectDockerContainerBlkio(query);
        // Mapping of Values

        SELECT * FROM "docker_container_blkio" WHERE time > now() -100s AND "io.kubernetes.pod.uid" = '793a8ef8-dc42-4491-95fe-6ba3437038ba'
                PodSandbox
        // SELECT * FROM "docker_container_blkio" WHERE time > now() -100s AND ("io.kubernetes.pod.uid" = '793a8ef8-dc42-4491-95fe-6ba3437038ba' OR "io.kubernetes.pod.uid" = 'a81cc2b1-ce2d-4224-8d37-8008926b43e3')
        blkioList.forEach(b -> {
            Metrics2 m = new Metrics2();
            m.setIoServiceRecursiveWrite(b.getIoServiceRecursiveWrite());
            m.setIoServiceRecursiveRead(b.getIoServiceRecursiveRead());
        });
         */
        return metrics2List;
    }

    public List<Metrics2> getMetricsFromKubernetesPodContainer(int seconds, List<String> labels) {
        return null;
    }

    public List<Metrics2> getMetricsFromKubernetesPodNetwork(int seconds, List<String> labels) {
        return null;
    }

    public List<Metrics2> getMetricsFromKubernetesPodVolume(int seconds, List<String> labels) {
        return null;
    }
}
