package de.lukonjun.metricscollector.ml;

import de.lukonjun.metricscollector.data.DataAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class J48AnomalyDetector {

    Logger logger = LoggerFactory.getLogger(J48AnomalyDetector.class);

    public List<Sample> createTestData(int numberOfData){

        List<Sample> samples = new ArrayList<>();
        List<String> labels = allClasses();

        for(int i = 0; i < numberOfData; i ++){
            Sample s = new Sample();
            samples.add(s);
            String randomLabel = labels.get(ThreadLocalRandom.current().nextInt(0, labels.size()));
            // "runningTimeSeconds imageSizeBytes cpuUsageNanoCores memoryUsageBytes label
            double [] metricsArray = {new Random().nextDouble(),new Random().nextDouble(),new Random().nextDouble(),new Random().nextDouble()};
            s.setMetricsArray(metricsArray);
            s.setLabel(randomLabel);
        }
        return samples;
    }

    public static void main(String[] args) throws Exception {
        // Generate Test Data
        J48AnomalyDetector ad = new J48AnomalyDetector();
        List<Sample> trainingSamples = ad.createTestData(5);

        // Init weka model
        Instances instances = ad.createDataset(trainingSamples);
        ad.fillDataset(instances, trainingSamples);
        // Train weka model
        J48 wekaModel = new J48();

        /*
        String[] options = new String[4];
        options[0] = "-C";
        options[1] = "0.1";
        options[2] = "-M";
        options[3] = "2";
        wekaModel.setOptions(options);
        */

        wekaModel.buildClassifier(instances);

        System.out.println("Print some Result Information of the trained model here");

    }

    public Instances createDataset(List<Sample> trainingSamples) {
        if (trainingSamples.isEmpty()) {
            throw new IllegalStateException("Cannot create empty dataset");
        }
        Instances instances = new Instances(toString() + " data", new ArrayList<>(), trainingSamples.size());

        for (String field : trainingSamples.get(0).getHeaderList()) {
            Attribute attr = new Attribute(field);
            instances.insertAttributeAt(attr,instances.numAttributes());
            //System.out.println(instances.numAttributes());
        }
        Attribute attr = new Attribute("class", allClasses());
        instances.insertAttributeAt(attr, instances.numAttributes()); // Does this work with Strings?
        // System.out.println(instances.numAttributes());
        instances.setClass(instances.attribute(instances.numAttributes() - 1));
        return instances;
    }

    //All possible classes (different labels) have to be defined at the beginning. Here its just 0 and 1 because of simple anomaly detection (anomaly or normal)
    public List<String> allClasses() {
        return Arrays.asList("mysql","nginx","mongodb","postgresql","apache");
    }

    public synchronized void fillDataset(Instances instances, List<Sample> trainingSamples) {
        //logger.info("Train-values: " + trainingSamples.get(0).getHeaderList().toString());

        Map<String, Integer> numberMapping = new HashMap<>();
        // Adding key-value pairs to a HashMap
        numberMapping.put("mysql", 0);
        numberMapping.put("nginx", 1);
        numberMapping.put("mongodb", 2);
        numberMapping.put("postgresql", 3);
        numberMapping.put("apache", 4);

        for (Sample sample : trainingSamples) {

            double[] values = sample.getMetricsArray();

            values = Arrays.copyOf(values, values.length + 1);
            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(instances);
            instance.setClassValue((double) numberMapping.get(sample.getLabel()));
            instances.add(instance);
        }
    }

    public String inputInstanceIntoModel(J48 wekaModel, Instance instance) throws Exception {
        double result = wekaModel.classifyInstance(instance);
        //Get predicted label
        String label = instance.classAttribute().value((int) result);
        return label;
    }

    public Instances createDatasetWithFilter(List<Sample> trainingSamples, boolean[] metricsOnlyNumbersFilter) {
        if (trainingSamples.isEmpty()) {
            throw new IllegalStateException("Cannot create empty dataset");
        }
        Instances instances = new Instances(toString() + " data", new ArrayList<>(), trainingSamples.size());

        int index = 0;
        for (String field : trainingSamples.get(0).getHeaderList()) {
            if(metricsOnlyNumbersFilter == null || metricsOnlyNumbersFilter[index] == true) {
                Attribute attr = new Attribute(field);
                instances.insertAttributeAt(attr, instances.numAttributes());
                //System.out.println(instances.numAttributes());
            }
            index++;
        }
        Attribute attr = new Attribute("class", allClasses());
        instances.insertAttributeAt(attr, instances.numAttributes()); // Does this work with Strings?
        // System.out.println(instances.numAttributes());
        instances.setClass(instances.attribute(instances.numAttributes() - 1));
        return instances;
    }

    public synchronized void fillDatasetWithFilter(Instances instances, List<Sample> trainingSamples, boolean[] filter) {
        //logger.info("Train-values: " + trainingSamples.get(0).getHeaderList().toString());

        Map<String, Integer> numberMapping = new HashMap<>();
        // Adding key-value pairs to a HashMap
        numberMapping.put("mysql", 0);
        numberMapping.put("nginx", 1);
        numberMapping.put("mongodb", 2);
        numberMapping.put("postgresql", 3);
        numberMapping.put("apache", 4);

        for (Sample sample : trainingSamples) {

            double[] values = sample.getMetricsArray();

            values = Arrays.copyOf(values, values.length + 1);
            if(filter != null){
                int count = 0;
                for(boolean b:filter){
                    if(b == true) count++;
                }
                // + 1 for classifier
                double [] filteredValues = new double[count +1];

                int countArray = 0;
                for(int i = 0; i < values.length - 1; i++){
                    if(filter[i] == true){
                        filteredValues[countArray] = values[i];
                        countArray++;
                    }
                }
                values = filteredValues;
            }
            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(instances);
            instance.setClassValue((double) numberMapping.get(sample.getLabel()));
            instances.add(instance);
        }
    }
}
