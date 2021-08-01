package de.lukonjun.podwatcher.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetricsFilter {

    private boolean[] filter;
    private String name;

    private final String [] headerList = new String[] {
            "podName","namespace","memoryUsageBytes","cpuUsageNanocores",
            "logsfsUsedBytes","rootfsUsedBytes","imageSizeBytes", "imageName"
            ,"containerName","rx_bytes","tx_bytes", "ioServiceRecursiveRead",
            "ioServiceRecursiveWrite","usedBytesVolume","runningTimeSeconds"
    };

    public MetricsFilter(boolean[] filter, String name) {
        this.filter = filter;
        this.name = name;
    }

    public boolean[] getFilter() {
        return filter;
    }

    public void setFilter(boolean[] filter) {
        this.filter = filter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        List<String> stringList = new ArrayList<>();
        int index = 0;
        for(boolean b:filter){
            if(b == true){
                stringList.add(headerList[index]);
            }
            index++;
        }

        return "MetricsFilter{" + Arrays.toString(new List[]{stringList}) + '}';
    }
}
