package de.lukonjun.metricscollector.ml;

import java.util.HashMap;
import java.util.Map;

public class WordToVectorTestClass {

    static String [] podNames = new String[]{"nginx","nginx","wso2am-mysql-db-service","release-t3n-mysql","nginx","nginx","nginx","nginx","my-mysql","nginx","nginx","nginx","nginx","mysql","my-mysql-t3n","mysql","release-wso2-mysql","nginx","mysql","apache","release-microfunctions-mongodb","nginx","release-bitnami-postgresql","release-wso2-mysql","release-t3n-mysql","nginx","nginx","mongodb","nginx","mongodb"};

    public static void main(String[] args) {

        for(int i = 0; i < podNames.length; i++){
            Double s = (double) podNames[i].hashCode();
            System.out.println(s);
        }

    }

}
