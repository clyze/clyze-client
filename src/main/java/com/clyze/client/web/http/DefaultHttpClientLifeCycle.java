package com.clyze.client.web.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class DefaultHttpClientLifeCycle implements HttpClientLifeCycle {
    public CloseableHttpClient createHttpClient() {
        int hourInMillis = 1000 * 60 * 60;
        RequestConfig config = RequestConfig.custom().setSocketTimeout(hourInMillis).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public void closeHttpClient(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
