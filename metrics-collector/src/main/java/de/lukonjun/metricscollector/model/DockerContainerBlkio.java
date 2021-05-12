package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Measurement(name = "docker_container_blkio")
public class DockerContainerBlkio extends Metrics{

    @Column(name = "io_serviced_recursive_read")
    private Long ioServiceRecursiveRead;

    @Column(name = "io_serviced_recursive_write")
    private Long ioServiceRecursiveWrite;

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
