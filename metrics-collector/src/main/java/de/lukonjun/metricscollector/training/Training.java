package de.lukonjun.metricscollector.training;

import de.lukonjun.metricscollector.data.DataAggregator;
import de.lukonjun.metricscollector.ml.Sample;
import de.lukonjun.metricscollector.model.MetricsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.common.io.Files;

@Component
public class Training {

    public class Plot {
        private int x_instances;
        private double y_accuracy;

        public int getX_instances() {
            return x_instances;
        }

        public void setX_instances(int x_instances) {
            this.x_instances = x_instances;
        }

        public double getY_accuracy() {
            return y_accuracy;
        }

        public void setY_accuracy(double y_accuracy) {
            this.y_accuracy = y_accuracy;
        }
    }

    Logger logger = LoggerFactory.getLogger(Training.class);

    @Value("${training.iterations:10}")
    private int iterations;

    @Value("${training.ratio:0.8}")
    private float ratioTrainingValidation;

    @Value("#{'${data.aggregator.decision.tree.classifier.list}'.split(',')}")
    private List<String> labels;

    @Value("${training.maxNumber:100}")
    private int trainingMaxNumber;

    @Autowired
    DataAggregator dataAggregator;

    // @Scheduled(fixedRateString = "${data.aggregator.decision.tree.interval:10000}")
    public void trainValidateAndPlot() throws Exception {

        boolean normalize = false; // normalize data (Value between 0 and 1) or not
        int maxNumberTrainings = trainingMaxNumber; // train with 1 - X Datasets, starting with 1,2,3 and so on

        int countTotalValidationErrors;
        int countCorrectValidations;
        // Confusion Matrix
        int [][] confMatrix;

        // TODO Specific Filter
        List<MetricsFilter> filterList = new ArrayList<>();
        // filterList.add(new MetricsFilter(new boolean[]{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false}, "only-memory"));
        // filterList.add(new MetricsFilter(new boolean[]{false,false,false,true,false,false,false,false,false,false,false,false,false,false,false}, "only-cpu"));
        // filterList.add(new MetricsFilter(new boolean[]{false,false,true,true,true,true,false,false,false,true,true,true,true,true,true}, "only-numbers-no-image-size"));
        // filterList.add(new MetricsFilter(new boolean[]{false,false,false,false,false,false,false,false,false,false,false,false,false,true,false}, "only-usedBytesVolume"));
        filterList.add(new MetricsFilter(new boolean[]{true,true,true,true,true,true,true,true,true,true,true,true,true,true,true}, "only-podName"));

        for(MetricsFilter metricsFilter:filterList) {

            confMatrix = new int[5][5];

            // Set Time interval via metrics.interval.seconds in application properties
            ArrayList<Sample> trainingList = new ArrayList<>();
            ArrayList<Sample> validationList = new ArrayList<>();
            dataAggregator.generateTrainingSet(0.5F, trainingList, validationList);


            ArrayList<Plot> plotArrayList = new ArrayList<>();
            for (int i = 1; i <= maxNumberTrainings; i++) {
                countTotalValidationErrors = 0;
                countCorrectValidations = 0;

                Plot plot = new Plot();
                plot.setX_instances(i);

                // Train Model
                // Take only Number of Trainings from trainingList
                ArrayList<Sample> trainingListLimit = new ArrayList<>();
                for (int j = 0; j < i; j++) {
                    int randomNum = ThreadLocalRandom.current().nextInt(0, trainingList.size() - 1);
                    trainingListLimit.add(trainingList.get(randomNum));
                }

                File file  = createEmptyTempFile("tmp_file");
                System.out.println("Path of the file where the serialized model is stored " + file.getAbsolutePath());
                J48 wekaModel = dataAggregator.trainModel(metricsFilter, trainingListLimit, normalize);
                weka.core.SerializationHelper.write(file.getAbsolutePath(), wekaModel);
                System.out.println(wekaModel);

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                // Validate

                // TODO Create Instances of Training Set Here, with Filter, iterate over them, and validate then

                Instances validationData = dataAggregator.getInstances(metricsFilter,validationList, normalize);
                /*
                Instances instances;
                instances.size();
                instances.get();
                */
                for (int j = 0; j < validationData.size(); j++) {
                    //for (Sample s : validationList) {
                    String labelValidation = dataAggregator.validateInstance(wekaModel,validationData.get(j));
                    //String labelValidation = dataAggregator.validateReturnString(s, wekaModel, metricsFilter.getFilter(),normalize);
                    int classIndex = validationData.get(j).classIndex();
                    String classifier = validationData.get(j).toString(classIndex);
                    if (classifier.equals(labelValidation)) {
                        logger.debug("Correct Validation");
                        countCorrectValidations++;
                    } else {
                        logger.debug("False Validation");
                        countTotalValidationErrors++;
                    }
                    String outLabel = labelValidation;
                    String actualLabel = classifier;
                    int outLabelIndex = this.labels.indexOf(outLabel);
                    int actualLabelIndex = this.labels.indexOf(actualLabel);
                    confMatrix[actualLabelIndex][outLabelIndex] += 1;

                }
                double accuracy = (double) countCorrectValidations / (double) validationData.size();
                System.out.println(accuracy);
                plot.setY_accuracy(accuracy);
                plotArrayList.add(plot);
            }

            System.out.println("Print Plot Points");
            for(Plot plot:plotArrayList){
                // Data Points for LaTeX
                System.out.print("Instances : " + plot.x_instances + " ");
                System.out.println("Accuracy " + plot.y_accuracy);
            }
            System.out.println("Plot for LaTeX");
            for(Plot plot:plotArrayList){
                // Data Points for LaTeX
                System.out.println(plot.x_instances + " " + plot.y_accuracy);
            }
            System.out.println("Build Average");
            double accuracySum = 0;
            int space = 10;
            for(Plot plot:plotArrayList){
                // Data Points for LaTeX
                accuracySum = accuracySum + plot.getY_accuracy();
                if(plot.getX_instances() % space == 0){
                    System.out.println(plot.x_instances + " " + accuracySum/space);
                    accuracySum = 0;
                }
            }
            int x = 1;
        }
    }

    public static File createEmptyTempFile(String fileName) throws IOException {

        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        File fileWithAbsolutePath = new File(tempDirectory.getAbsolutePath() + fileName);

        Files.touch(fileWithAbsolutePath);

        return fileWithAbsolutePath;
    }

    @Scheduled(fixedRateString = "${data.aggregator.decision.tree.interval:10000}")
    public void trainAndValidate() throws Exception {
        // normalize data (Value between 0 and 1) or nor
        boolean normalize = true;

        int countTotalValidationErrors;
        int countCorrectValidations;
        // Confusion Matrix
        int [][] confMatrix;

        // TODO Specific Filter
        List<MetricsFilter> filterList = new ArrayList<>();
        //filterList.add(new MetricsFilter(new boolean[]{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false}, "only-memory"));
        //filterList.add(new MetricsFilter(new boolean[]{false,false,false,true,false,false,false,false,false,false,false,false,false,false,false}, "only-cpu"));
        //filterList.add(new MetricsFilter(new boolean[]{false,false,true,true,true,true,false,false,false,true,true,true,true,true,true}, "only-numbers-no-image-size"));
        //filterList.add(new MetricsFilter(new boolean[]{false,false,false,false,false,false,false,false,false,false,false,false,false,true,false}, "only-usedBytesVolume"));
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
                J48 wekaModel = dataAggregator.trainModel(metricsFilter, trainingList, normalize);
                File file  = createEmptyTempFile("tmp_file");
                System.out.println("Path of the file where the serialized model is stored " + file.getAbsolutePath());
                weka.core.SerializationHelper.write(file.getAbsolutePath(), wekaModel);
                System.out.println(wekaModel);

                // https://www.baeldung.com/java-base64-encode-and-decode
                // Print Base64 String of File
                StringBuilder stringBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                }
                String encodedString = Base64.getEncoder().encodeToString(stringBuilder.toString().getBytes());
                /*
                System.out.println("---- BASE64 ENCODED MODEL START ----");
                System.out.println(encodedString);
                System.out.println("---- BASE64 ENCODED MODEL START ----");
                */
                // weka.core.SerializationHelper.write(  pathSerializedFile + "/" + metricsFilter.getName() + ".model", wekaModel);
                // Validate
                // System.out.println("Path of the serialized Model is also: " + pathSerializedFile + "/" + metricsFilter.getName() + ".model");
                // TODO Create Instances of Training Set Here, with Filter, iterate over them, and validate then

                Instances validationData = dataAggregator.getInstances(metricsFilter,validationList, normalize);
                /*
                Instances instances;
                instances.size();
                instances.get();
                */
                for (int j = 0; j < validationData.size(); j++) {
                //for (Sample s : validationList) {
                    String labelValidation = dataAggregator.validateInstance(wekaModel,validationData.get(j));
                    //String labelValidation = dataAggregator.validateReturnString(s, wekaModel, metricsFilter.getFilter(),normalize);
                    int classIndex = validationData.get(j).classIndex();
                    String classifier = validationData.get(j).toString(classIndex);
                    if (classifier.equals(labelValidation)) {
                        logger.debug("Correct Validation");
                        countCorrectValidations++;
                    } else {
                        logger.debug("False Validation");
                        countTotalValidationErrors++;
                    }
                    String outLabel = labelValidation;
                    String actualLabel = classifier;
                    int outLabelIndex = this.labels.indexOf(outLabel);
                    int actualLabelIndex = this.labels.indexOf(actualLabel);
                    confMatrix[actualLabelIndex][outLabelIndex] += 1;
                }
                // Build Confusion Matrix
            }
            logger.info("Report for " + metricsFilter);
            logger.info("--------------------------------------------------------------");
            logger.info("Total Count Validation Errors: " + countTotalValidationErrors);
            logger.info("Total Count Correct Validation: " + countCorrectValidations);
            System.out.println();
            System.out.println("Confusion Matrix");
            System.out.println("--------------------------------------------------------------");
            System.out.println("y axis contains actual class (input), -> x axis contains predictions (output)");
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

        System.out.println();
        System.out.println("Performance Measurements");
        System.out.println("Accuracy for the Model is " + accuracy);
        System.out.println("Average Precision for the Model is " + averagePrecision);
        System.out.println("Average Recall for the Model is " + averageRecall);
        System.out.println("F1 Score for the Model is " + f1Score);
        System.out.println();
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
