package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "kubernetes_pod_network")
public class KubernetesPodNetwork extends Metrics {

    @Column(name = "time")
    private Instant time;

    @Column(name = "rx_bytes")
    private Long rxBytes;

    @Column(name = "tx_bytes")
    private Long txBytes;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public void setRxBytes(Long rxBytes) {
        this.rxBytes = rxBytes;
    }

    public void setTxBytes(Long txBytes) {
        this.txBytes = txBytes;
    }

    public Long getRxBytes() {
        return rxBytes;
    }

    public Long getTxBytes() {
        return txBytes;
    }

}
