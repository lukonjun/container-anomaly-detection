package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Measurement(name = "kubernetes_pod_network")
public class KubernetesPodNetwork extends Metrics {

    @Column(name = "rx_bytes")
    private Long rxBytes;

    @Column(name = "tx_bytes")
    private Long txBytes;

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
