package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "kubernetes_pod_volume")
public class KubernetesPodVolume extends Metrics {

    @Column(name = "time")
    private Instant time;

    @Column(name = "used_bytes")
    private Long usedBytes;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
    }

}
