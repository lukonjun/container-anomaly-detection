package de.lukonjun.metricscollector.ml;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

@Component
public class DecisionTree {

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

}
