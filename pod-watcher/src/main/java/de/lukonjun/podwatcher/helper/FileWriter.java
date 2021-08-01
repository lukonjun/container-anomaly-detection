package de.lukonjun.podwatcher.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class FileWriter {

    static Logger logger = LoggerFactory.getLogger(FileWriter.class);

    // From https://waikato.github.io/weka-wiki/formats_and_processing/save_instances_to_arff/
    public static void writeArffFile(Instances instances, String path, String filename) throws IOException {
        String absolutePath = path + filename + ".arff";
        ArffSaver saver = new ArffSaver();
        saver.setInstances(instances);
        saver.setFile(new File(absolutePath));
        saver.writeBatch();
        logger.info("Write .arff File " + path + filename + ".arff");
    }

    public static void writeToFile(String str, String filePath) throws IOException{

        BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(filePath));
        writer.write(str);

        writer.close();

    }

}
