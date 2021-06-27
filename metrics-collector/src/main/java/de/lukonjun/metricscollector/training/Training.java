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
            logger.info("--------------------------------------------------------------");
            logger.info("y axis contains actual class (input), -> x axis contains predictions (output)");
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

            performanceMeasurement(confMatrix);

        }
    }

    private void performanceMeasurement(int[][] confMatrix) {

        int totalDataset = calculateTotalDataset(confMatrix);
        int numberCorrectPredictions = calculateNumberCorrectPredictions(confMatrix);
        int truePositives [] = calculateTruePositives(confMatrix);
        int falsePositives [] = calculateFalsePositives(confMatrix);
        int falseNegative [] = calculatefalseNegative(confMatrix);

        // Accuracy (works well on balanced data)
        // correctly identified prediction for each class / total dataset
        double accuracy = (double) numberCorrectPredictions / (double) totalDataset;

        // Precision
        // TP / (TP + FP)
        // Execute above for each classifier, then average Precision
        double [] precision = new double[5];
        for (int i = 0; i < 5; i++) {
                precision[i] = (double) truePositives[i] / (double)(truePositives[i]+falsePositives[i]);
        }
        double averagePrecision = getAverage(precision);

        // Recall
        // TP / (TP + FN)
        double [] recall = new double[5];
        for (int i = 0; i < 5; i++) {
            recall[i] = (double)truePositives[i] / (double)(truePositives[i]+falseNegative[i]);
        }
        double averageRecall = getAverage(recall);

        // F1 Score (Good Metric when the data is imbalanced)
        // based on recall and precision
        double f1Score = 2 * ((averagePrecision * averageRecall)/(averagePrecision + averageRecall));

        System.out.println("Performance Measurements");
        System.out.println("Accuracy for the Model is " + accuracy);
        System.out.println("Average Precision for the Model is " + averagePrecision);
        System.out.println("Average Recall for the Model is " + averageRecall);
        System.out.println("F1 Score for the Model is " + f1Score);
    }

    private double getAverage(double[] precision) {
        double sum = 0;
        for (double value: precision) {
            sum += value;
        }
        return sum / precision.length;
    }

    public static double findAverageWithoutUsingStream(double[] array) {
        double sum = findSumWithoutUsingStream(array);
        return (double) sum / array.length;
    }

    public static double findSumWithoutUsingStream(double[] array) {
        int sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }

    private int[] calculatefalseNegative(int[][] confMatrix) {
        int[] falseNegative = new int[5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if(i != j) {
                    falseNegative[i] += confMatrix[i][j];
                }
            }
        }
        return falseNegative;
    }

    private int[] calculateFalsePositives(int[][] confMatrix) {
        int falsePositives [] = new int[5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if(i != j) {
                    falsePositives[i] += confMatrix[j][i];
                }
            }
        }
        return falsePositives;
    }

    private int[] calculateTruePositives(int[][] confMatrix) {
        int truePositives []= new int[5];
        for (int i = 0; i < 5; i++) {
            truePositives[i]=confMatrix[i][i];
        }
        return truePositives;
    }

    private int calculateNumberCorrectPredictions(int[][] confMatrix) {
        int numberCorrectPredictions = 0;
        for (int i = 0; i < 5; i++) {
            numberCorrectPredictions = numberCorrectPredictions + confMatrix[i][i];
        }
        return numberCorrectPredictions;
    }

    private int calculateTotalDataset(int[][] confMatrix) {

        int totalDataset = 0;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                totalDataset = totalDataset + confMatrix[i][j];
            }
        }

        return totalDataset;
    }

}
