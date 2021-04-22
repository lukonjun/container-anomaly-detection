package de.lukonjun.metricscollector.Pojo;


public class MetricsPojo {

    private String podName;

    private String containerName;

    private String namespace;

    private Long memoryBytes;

    private Double cpu;

    private Long imageSizeBytes;

    private String image;

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setMemoryBytes(Long memoryBytes) {
        this.memoryBytes = memoryBytes;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public void setImageSizeBytes(Long imageSizeBytes) {
        this.imageSizeBytes = imageSizeBytes;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPodName() {
        return podName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getNamespace() {
        return namespace;
    }

    public Long getMemoryBytes() {
        return memoryBytes;
    }

    public Double getCpu() {
        return cpu;
    }

    public Long getImageSizeBytes() {
        return imageSizeBytes;
    }

    public String getImage() {
        return image;
    }
}
