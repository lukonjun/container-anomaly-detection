package de.lukonjun.metricscollector.model;

public class MetricsFilter {

    private boolean[] filter;
    private String name;

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
}
