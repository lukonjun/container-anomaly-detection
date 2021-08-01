package de.lukonjun.metricscollector.ml;

import java.time.Instant;
import java.util.List;

public class Sample {

    private final String [] headerList = new String[] {
            "podName","namespace","memoryUsageBytes","cpuUsageNanocores",
            "logsfsUsedBytes","rootfsUsedBytes","imageSizeBytes", "imageName"
            ,"containerName","rx_bytes","tx_bytes", "ioServiceRecursiveRead",
            "ioServiceRecursiveWrite","usedBytesVolume","runningTimeSeconds"
    };

    private double [] metricsArray;
    private String label;

    public String[] getHeaderList() {
        return headerList;
    }

    public double[] getMetricsArray() {
        return metricsArray;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setMetricsArray(double[] metricsArray) {
        this.metricsArray = metricsArray;
    }
}
