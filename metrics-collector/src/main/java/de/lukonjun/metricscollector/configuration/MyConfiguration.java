package de.lukonjun.metricscollector.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MyConfiguration {

    // passing the key which you set in application.properties
    @Value("#{'${data.aggregator.decision.tree.classifier.list}'.split(',')}")
    private List<String> labels;

    @Bean
    public List<String> getLabels() {
        return labels;
    }

}
