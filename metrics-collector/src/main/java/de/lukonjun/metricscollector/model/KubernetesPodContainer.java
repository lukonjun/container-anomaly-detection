package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "kubernetes_pod_container")
public class KubernetesPodContainer extends Metrics {

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;

    @Column(name = "cpu_usage_nanocores")
    private Long cpuUsageNanocores;

    @Column(name = "logsfs_used_bytes")
    private Long logsfsUsedBytes;

    @Column(name = "rootfs_used_bytes")
    private Long rootfsUsedBytes;

    @Override
    public String toString() {
        return podName + " " + namespace + " " + memoryUsageBytes + " " + cpuUsageNanocores + " " + logsfsUsedBytes + " " + rootfsUsedBytes;
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
}


