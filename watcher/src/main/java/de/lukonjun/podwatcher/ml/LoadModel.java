package de.lukonjun.podwatcher.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.J48;

@Component
public class LoadModel {

    Logger logger = LoggerFactory.getLogger(LoadModel.class);

    private J48 wekaModel;


    public LoadModel() throws Exception {
        this.wekaModel = (J48) weka.core.SerializationHelper.read("/tmp/weka_model");
        logger.info("Loaded Model " +  this.wekaModel);
    }

    public J48 getWekaModel() {
        return wekaModel;
    }

}