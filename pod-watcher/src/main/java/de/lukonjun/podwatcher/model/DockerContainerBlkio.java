package de.lukonjun.podwatcher.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "docker_container_blkio")
public class DockerContainerBlkio extends Metrics{

    @Column(name = "time")
    private Instant time;

    @Column(name = "io_serviced_recursive_read")
    private Long ioServiceRecursiveRead;

    @Column(name = "io_serviced_recursive_write")
    private Long ioServiceRecursiveWrite;

    @Column(name = "io.kubernetes.container.name")
    private String containerName;

    @Column(name = "io.kubernetes.pod.name")
    private String podName;

    @Column(name = "io.kubernetes.pod.namespace")
    private String podNamespace;

    @Column(name = "io.kubernetes.pod.uid")
    private String podUid;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getPodNamespace() {
        return podNamespace;
    }

    public void setPodNamespace(String podNamespace) {
        this.podNamespace = podNamespace;
    }

    public String getPodUid() {
        return podUid;
    }

    public void setPodUid(String podUid) {
        this.podUid = podUid;
    }

    public void setIoServiceRecursiveRead(Long ioServiceRecursiveRead) {
        this.ioServiceRecursiveRead = ioServiceRecursiveRead;
    }

    public void setIoServiceRecursiveWrite(Long ioServiceRecursiveWrite) {
        this.ioServiceRecursiveWrite = ioServiceRecursiveWrite;
    }

    public Long getIoServiceRecursiveRead() {
        return ioServiceRecursiveRead;
    }

    public Long getIoServiceRecursiveWrite() {
        return ioServiceRecursiveWrite;
    }
}
