package de.lukonjun.metricscollector.ml;

import de.lukonjun.metricscollector.data.DataAggregator;
import de.lukonjun.metricscollector.model.Metrics2;
import de.lukonjun.metricscollector.pojo.MetricsPojo;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@Component
public class DecisionTree {

    @Autowired
    DataAggregator dataAggregator;

    //@Scheduled(fixedRateString = "1000")
    public void buildDecisionTree() throws Exception {

        J48 tree = new J48(); // new instance of tree

        // Provide Instances via arff file
        String filePathWeather = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/weather.arff";
        String filePathTestData = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/test-data.arff";

        BufferedReader reader = new BufferedReader(new FileReader(filePathWeather));
        Instances data = new Instances(reader);
        // setting class attribute
        data.setClassIndex(data.numAttributes() - 1);
        reader.close();

        tree.buildClassifier(data); // build classifier
        System.out.println(tree.globalInfo());

    }

    private Instances createDataset() {

        Instances instances = new Instances(toString() + " data", new ArrayList<>(), Metrics2.variableNames.length);
        for (String field : Metrics2.variableNames) {
            instances.insertAttributeAt(new Attribute(field), instances.numAttributes());
        }
        Attribute attr = new Attribute("class", allClasses());
        instances.insertAttributeAt(attr, instances.numAttributes());
        instances.setClass(instances.attribute(instances.numAttributes() - 1));
        return instances;
    }

    //All possible classes (different labels) have to be defined at the beginning. Here its just 0 and 1 because of simple anomaly detection (anomaly or normal)
    private List<String> allClasses() {
        return Arrays.asList("nginx", "mysql");
    }

}
