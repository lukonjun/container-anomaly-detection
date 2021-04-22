package de.lukonjun.metricscollector.model;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "pod_metrics")
public class MetricsPoint {

    @Column(name = "time")
    private Instant time;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "memory_bytes")
    private Long memoryBytes;

    @Column(name = "cpu")
    private Double cpu;

    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    @Column(name = "image")
    private String image;

}