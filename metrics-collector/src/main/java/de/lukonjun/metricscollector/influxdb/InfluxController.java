package de.lukonjun.metricscollector.influxdb;

import de.lukonjun.metricscollector.model.*;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    public List<KubernetesPodVolume> selectFromKubernetesPodVolume(String query){
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

    public List<Metrics> getMetricsFromDockerContainerBlkio(int seconds, Metrics m) {
        String query = "SELECT * FROM \"docker_container_blkio\" WHERE time > now() -" + seconds + "s AND (\"io.kubernetes.container.name\" ='" + m.getContainerName() + "') AND (\"io.kubernetes.pod.uid\" ='" + m.getPodUid() +"') AND (\"io.kubernetes.docker.type\" ='container') AND (\"device\" ='total')";
        logger.debug("Generated Influxql query: " + query);

        List<DockerContainerBlkio> containerBlkioList = selectDockerContainerBlkio(query);
        List<Metrics> metricsList = new ArrayList<>();

        containerBlkioList.forEach(b -> {
            Metrics metricPoint = new Metrics();
            metricPoint.setTime(b.getTime());
            metricPoint.setPodUid(m.getPodUid());
            metricPoint.setContainerName(m.getContainerName());
            // Specifc Metrics
            metricPoint.setIoServiceRecursiveWrite(b.getIoServiceRecursiveWrite());
            metricPoint.setIoServiceRecursiveRead(b.getIoServiceRecursiveRead());
            metricsList.add(metricPoint);
        });

        return metricsList;
    }

    public List<Metrics> getMetricsFromKubernetesPodContainer(int seconds, Metrics m) {
        String query = "SELECT * FROM \"kubernetes_pod_container\" WHERE time > now() -" + seconds + "s AND (\"container_name\" ='" + m.getContainerName() + "') AND (\"pod_name\" ='" + m.getPodName() +"') AND (\"namespace\" ='" + m.getNamespace() + "')";
        logger.debug("Generated Influxql query: " + query);

        List<KubernetesPodContainer> podContainerList = selectFromKubernetesPodContainer(query);
        List<Metrics> metricsList = new ArrayList<>();

        podContainerList.forEach(p -> {
            Metrics metricPoint = new Metrics();
            metricPoint.setTime(p.getTime());
            metricPoint.setPodUid(m.getPodUid());
            metricPoint.setContainerName(m.getContainerName());
            // Specifc Metrics
            metricPoint.setMemoryUsageBytes(p.getMemoryUsageBytes());
            metricPoint.setCpuUsageNanocores(p.getCpuUsageNanocores());
            metricPoint.setLogsfsUsedBytes(p.getLogsfsUsedBytes());
            metricPoint.setRootfsUsedBytes(p.getRootfsUsedBytes());
            metricsList.add(metricPoint);
        });

        return metricsList;
    }

    public List<Metrics> getMetricsFromKubernetesPodNetwork(int seconds, Metrics m) {
        String query = "SELECT * FROM \"kubernetes_pod_network\" WHERE time > now() -" + seconds + "s AND (\"pod_name\" ='" + m.getPodName() +"') AND (\"namespace\" ='" + m.getNamespace() + "')";
        logger.debug("Generated Influxql query: " + query);

        List<KubernetesPodNetwork> kubernetesPodNetworkList = selectFromKubernetesPodNetwork(query);
        List<Metrics> metricsList = new ArrayList<>();

        kubernetesPodNetworkList.forEach(p -> {
            Metrics metricPoint = new Metrics();
            metricPoint.setTime(p.getTime());
            metricPoint.setPodUid(m.getPodUid());
            metricPoint.setContainerName(m.getContainerName());
            // Specifc Metrics
            metricPoint.setRx_bytes(p.getRxBytes());
            metricPoint.setTx_bytes(p.getTxBytes());
            metricsList.add(metricPoint);
        });

        return metricsList;
    }

    public List<Metrics> getMetricsFromKubernetesPodVolume(int seconds, Metrics m) {
        String query = "SELECT * FROM \"kubernetes_pod_volume\" WHERE time > now() -" + seconds + "s AND (\"pod_name\" ='" + m.getPodName() +"') AND (\"namespace\" ='" + m.getNamespace() + "')";
        logger.debug("Generated Influxql query: " + query);

        List<KubernetesPodVolume> kubernetesPodVolumeList = selectFromKubernetesPodVolume(query);
        List<Metrics> metricsList = new ArrayList<>();

        kubernetesPodVolumeList.forEach(p -> {
            Metrics metricPoint = new Metrics();
            metricPoint.setTime(p.getTime());
            metricPoint.setPodUid(m.getPodUid());
            metricPoint.setContainerName(m.getContainerName());
            // Specifc Metrics
            metricPoint.setUsedBytesVolume(p.getUsedBytes());
            metricsList.add(metricPoint);
        });

        return metricsList;
    }
}
