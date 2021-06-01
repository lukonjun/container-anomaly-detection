package de.lukonjun.metricscollector.pojo;

public class ContainerImageInfoPojo {

    private String podUid;
    private String containerImageNameDigest;
    private String containerImageNameTag;
    private Long containerImageSizeBytes;

    public String getPodUid() {
        return podUid;
    }

    public void setPodUid(String podUid) {
        this.podUid = podUid;
    }

    public void setContainerImageNameDigest(String containerImageNameDigest) {
        this.containerImageNameDigest = containerImageNameDigest;
    }

    public void setContainerImageNameTag(String containerImageNameTag) {
        this.containerImageNameTag = containerImageNameTag;
    }

    public void setContainerImageSizeBytes(Long containerImageSizeBytes) {
        this.containerImageSizeBytes = containerImageSizeBytes;
    }

    public String getContainerImageNameDigest() {
        return containerImageNameDigest;
    }

    public String getContainerImageNameTag() {
        return containerImageNameTag;
    }

    public Long getContainerImageSizeBytes() {
        return containerImageSizeBytes;
    }
}
