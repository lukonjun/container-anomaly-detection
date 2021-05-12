package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Measurement(name = "kubernetes_pod_volume")
public class KubernetesPodVolume extends Metrics {

    @Column(name = "used_bytes")
    private Long usedBytes;

    public Long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
    }

}
