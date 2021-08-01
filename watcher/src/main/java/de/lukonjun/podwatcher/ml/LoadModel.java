package de.lukonjun.podwatcher.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;

@Component
public class LoadModel {

    Logger logger = LoggerFactory.getLogger(LoadModel.class);

    private J48 wekaModel;

    public LoadModel(@Value("${path.serialized.model}") String pathSerializedModel) throws Exception {
        this.wekaModel = (J48) weka.core.SerializationHelper.read(pathSerializedModel);
        logger.info("Loaded Model " +  this.wekaModel);
    }

    public J48 getWekaModel() {
        return wekaModel;
    }

}