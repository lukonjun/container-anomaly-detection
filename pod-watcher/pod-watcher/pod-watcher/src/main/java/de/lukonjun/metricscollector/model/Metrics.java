package de.lukonjun.metricscollector.model;

import io.kubernetes.client.proto.Meta;

import java.time.Instant;

public class Metrics {

    public static final String [] variableNames = {"label","cpuUsageNanocores","memoryUsageBytes"};

    private String label;

    private Instant time;

    private String podName;

    private String namespace;

    private Long memoryUsageBytes;

    private Long cpuUsageNanocores;

    private Long logsfsUsedBytes;

    private Long rootfsUsedBytes;

    private Long imageSizeBytes;

    private String imageName;

    private String containerName;

    private String podUid;

    private Long rx_bytes;

    private Long tx_bytes;

    private Long ioServiceRecursiveRead;

    private Long ioServiceRecursiveWrite;

    private Long usedBytesVolume;

    private Meta.Time startTime;

    private int runningTimeSeconds;

    public String getPodUid() {
        return podUid;
    }

    public void setPodUid(String podUid) {
        this.podUid = podUid;
    }

    public int getRunningTimeSeconds() {
        return runningTimeSeconds;
    }

    public void setRunningTimeSeconds(int runningTimeSeconds) {
        this.runningTimeSeconds = runningTimeSeconds;
    }

    public Meta.Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Meta.Time startTime) {
        this.startTime = startTime;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setMemoryUsageBytes(Long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }

    public void setCpuUsageNanocores(Long cpuUsageNanocores) {
        this.cpuUsageNanocores = cpuUsageNanocores;
    }

    public void setLogsfsUsedBytes(Long logsfsUsedBytes) {
        this.logsfsUsedBytes = logsfsUsedBytes;
    }

    public void setRootfsUsedBytes(Long rootfsUsedBytes) {
        this.rootfsUsedBytes = rootfsUsedBytes;
    }

    public void setImageSizeBytes(Long imageSizeBytes) {
        this.imageSizeBytes = imageSizeBytes;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setRx_bytes(Long rx_bytes) {
        this.rx_bytes = rx_bytes;
    }

    public void setTx_bytes(Long tx_bytes) {
        this.tx_bytes = tx_bytes;
    }

    public void setUsedBytesVolume(Long usedBytesVolume) {
        this.usedBytesVolume = usedBytesVolume;
    }

    public Long getIoServiceRecursiveRead() {
        return ioServiceRecursiveRead;
    }

    public void setIoServiceRecursiveRead(Long ioServiceRecursiveRead) {
        this.ioServiceRecursiveRead = ioServiceRecursiveRead;
    }

    public Long getIoServiceRecursiveWrite() {
        return ioServiceRecursiveWrite;
    }

    public void setIoServiceRecursiveWrite(Long ioServiceRecursiveWrite) {
        this.ioServiceRecursiveWrite = ioServiceRecursiveWrite;
    }

    public Instant getTime() {
        return time;
    }

    public String getPodName() {
        return podName;
    }

    public String getNamespace() {
        return namespace;
    }

    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    public Long getCpuUsageNanocores() {
        return cpuUsageNanocores;
    }

    public Long getLogsfsUsedBytes() {
        return logsfsUsedBytes;
    }

    public Long getRootfsUsedBytes() {
        return rootfsUsedBytes;
    }

    public Long getImageSizeBytes() {
        return imageSizeBytes;
    }

    public String getImageName() {
        return imageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public Long getRx_bytes() {
        return rx_bytes;
    }

    public Long getTx_bytes() {
        return tx_bytes;
    }

    public Long getUsedBytesVolume() {
        return usedBytesVolume;
    }
}
