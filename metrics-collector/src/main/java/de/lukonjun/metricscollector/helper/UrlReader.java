package de.lukonjun.metricscollector.helper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class UrlReader {

    // http://zetcode.com/java/readwebpage/
    public static String urlReader(String sourceUrl) throws IOException {

        HttpGet request = null;
        String content = null;

        try {
            String url = sourceUrl;

            int timeout = 5;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000).build();
            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            request = new HttpGet(url);
            request.addHeader("Content-Type","text/plain;charset=UTF-8");
            request.addHeader("User-Agent", "Apache HTTPClient");
            HttpResponse response = client.execute(request);

            HttpEntity entity = response.getEntity();
            content = EntityUtils.toString(entity, "UTF-8");

        }catch (Exception e) {
            e.printStackTrace(System.out);
        }
        finally {

            if (request != null) {

                request.releaseConnection();
            }
        }

        return content;
    }

}
