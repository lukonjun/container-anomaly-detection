package de.lukonjun.metricscollector.training;

import de.lukonjun.metricscollector.data.DataAggregator;
import de.lukonjun.metricscollector.ml.Sample;
import de.lukonjun.metricscollector.model.Metrics;
import de.lukonjun.metricscollector.model.MetricsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class Training {

    Logger logger = LoggerFactory.getLogger(Training.class);

    @Value("${training.iterations:10}")
    private int iterations;

    @Value("${training.ratio:0.8}")
    private float ratioTrainingValidation;

    @Autowired
    DataAggregator dataAggregator;

    @Scheduled(fixedRateString = "${data.aggregator.decision.tree:10000}")
    public void trainAndValidate() throws Exception {
        int countTotalValidationErrors;
        int countCorrectValidations;
        // Confusion Matrix
        int [][] confMatrix;
        String[] a = new String[] {"mysql","nginx","mongodb","postgresql","apache"};
        List<String> category = Arrays.asList(a);

        // TODO Specific Filter
        List<MetricsFilter> filterList = new ArrayList<>();
        filterList.add(new MetricsFilter(new boolean[]{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false}, "only-memory"));
        filterList.add(new MetricsFilter(new boolean[]{false,false,false,true,false,false,false,false,false,false,false,false,false,false,false}, "only-cpu"));
        filterList.add(new MetricsFilter(new boolean[]{false,false,true,true,true,true,false,false,false,true,true,true,true,true,true}, "only-numbers-no-image-size"));
        filterList.add(new MetricsFilter(new boolean[]{false,false,false,false,false,false,false,false,false,false,false,false,false,true,false}, "only-usedBytesVolume"));
        filterList.add(new MetricsFilter(new boolean[]{true,false,false,false,false,false,false,false,false,false,false,false,false,false,false}, "only-podName"));

        for(MetricsFilter metricsFilter:filterList) {
            countTotalValidationErrors = 0;
            countCorrectValidations = 0;
            confMatrix = new int[5][5];
            for (int i = 0; i < iterations; i++) {
                logger.info("Iteration: " + (i+1) + " Filter: " + metricsFilter);
                // Set Time interval via metrics.interval.seconds in application properties
                ArrayList<Sample> trainingList = new ArrayList<>();
                ArrayList<Sample> validationList = new ArrayList<>();
                dataAggregator.generateTrainingSet(ratioTrainingValidation, trainingList, validationList);
                // Train Model
                J48 wekaModel = dataAggregator.trainModel(metricsFilter, trainingList);
                weka.core.SerializationHelper.write("/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/blobs/" + metricsFilter.getName() + ".model", wekaModel);
                // Validate
                for (Sample s : validationList) {
                    String labelValidation = dataAggregator.validateReturnString(s, wekaModel, metricsFilter.getFilter());
                    if (s.getLabel().equals(labelValidation)) {
                        logger.debug("Correct Validation");
                        countCorrectValidations++;
                    } else {
                        logger.debug("False Validation");
                        countTotalValidationErrors++;
                    }
                    String outLabel = labelValidation;
                    String actualLabel = s.getLabel();
                    int outLabelIndex = category.indexOf(outLabel);
                    int actualLabelIndex = category.indexOf(actualLabel);
                    confMatrix[actualLabelIndex][outLabelIndex] += 1;
                }
                // Build Confusion Matrix
            }
            logger.info("Report for " + metricsFilter);
            logger.info("--------------------------------------------------------------");
            logger.info("Total Count Validation Errors: " + countTotalValidationErrors);
            logger.info("Total Count Correct Validation: " + countCorrectValidations);
            logger.info("--------------------------------------------------------------");
            logger.info("Confusion Matrix");
            // {"mysql","nginx","mongodb","postgresql","apache"};
            // Using Short Array, max length is 6
            String[] shortNames = new String[] {"mysql","nginx","mongo","psql","apache"};

            for (int i = -1; i < 5; i++) {
                for (int j = -1; j < 5; j++) {
                    if(i == -1 && j == -1) {
                        System.out.printf("%7s","");
                        continue;
                    }
                    if(i == -1){
                        System.out.printf("%7s", shortNames[j]);
                        continue;
                    }
                    if(j == -1){
                        System.out.printf("%7s", shortNames[i]);
                        continue;
                    }
                    if(i != -1 && j != -1) {
                        System.out.printf("%7d", confMatrix[i][j]);
                    }
                }
                System.out.println("");
            }
        }
    }
}
