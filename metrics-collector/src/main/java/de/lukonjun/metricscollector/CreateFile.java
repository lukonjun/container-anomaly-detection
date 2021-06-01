package de.lukonjun.metricscollector;

import com.google.common.io.Files;
import de.lukonjun.metricscollector.data.DataAggregator;
import de.lukonjun.metricscollector.model.Metrics2;
import io.kubernetes.client.openapi.ApiException;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import weka.gui.beans.InstanceListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.J48;
import weka.core.Instance;


@Component
@EnableScheduling
public class CreateFile {

    @Autowired
    DataAggregator dataAggregator;

    public File createEmtpyFile() throws IOException {

        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        File fileWithAbsolutePath = new File(tempDirectory.getAbsolutePath() + "/testFile.arff");

        Files.touch(fileWithAbsolutePath);

        return fileWithAbsolutePath;
    }

    //@Scheduled(fixedRateString = "10000")
    public void writingToFile() throws InterruptedException, ApiException, IOException {

        String absolutePath = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/container_metrics.arff";
        File file = new File(absolutePath);

        FileWriter fileWriter = new FileWriter(file);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("@relation container_metrics");
        printWriter.println("");
        printWriter.println("@attribute runningTimeSeconds numeric");
        printWriter.println("@attribute imageSizeBytes numeric");
        printWriter.println("@attribute cpuUsageNanocores numeric");
        printWriter.println("@attribute memoryUsageBytes numeric");
        printWriter.println("@attribute label {mysql, nginx}");
        printWriter.println("");
        printWriter.println("@data");

        List<String> labels = new ArrayList<>();
        labels.add("mysql");
        labels.add("nginx");
        labels.add("mongodb");
        labels.add("postgresql");
        labels.add("apache");

        dataAggregator.getMetrics(1, labels).forEach(m ->{
            // pod_name runningTimeSeconds imageSizeBytes cpuUsageNanocores memoryUsageBytes label
            printWriter.println(m.getRunningTimeSeconds() + "," + m.getImageSizeBytes() + "," +
                    m.getCpuUsageNanocores() + "," + m.getMemoryUsageBytes() + "," + m.getLabel());
        });

        printWriter.close();
        System.out.println("Absolute Path of File " + file.getAbsolutePath());
    }

    @Test
    public J48 testDataSet(){
        try {
            String absolutePath = "/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/container_metrics.arff";
            DataSource src = new DataSource(absolutePath);
            Instances dt = src.getDataSet();
            dt.setClassIndex(dt.numAttributes() - 1);

            String[] options = new String[4];
            options[0] = "-C";
            options[1] = "0.1";
            options[2] = "-M";
            options[3] = "2";
            J48 mytree = new J48();
            mytree.setOptions(options);
            mytree.buildClassifier(dt); // train model?

            weka.core.SerializationHelper.write("/Users/lucasstocksmeier/Coding/container-anomaly-detection/metrics-collector/src/main/resources/ml/my_tree.model", mytree);

            return mytree;
        }
        catch (Exception e) {
            System.out.println("Error!!!!\n" + e.getMessage());
            return null;
        }
    }


}
