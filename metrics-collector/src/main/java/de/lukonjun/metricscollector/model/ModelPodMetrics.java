package de.lukonjun.metricscollector.model;

import javax.persistence.*;

@Entity(name = "pod_metrics")
public class ModelPodMetrics {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String namespace;
    @Column(name="pod_name")
    private String podName;
    @Column(name="container_name")
    private String containerName;
    private int memory;
    private float cpu;

    public String getNamespace() {
        return namespace;
    }

    public String getPodName() {
        return podName;
    }

    public String getContainerName() {
        return containerName;
    }

    public int getMemory() {
        return memory;
    }

    public float getCpu() {
        return cpu;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public void setCpu(float cpu) {
        this.cpu = cpu;
    }

}
