package org.clyze.client.web.http

import groovy.transform.CompileStatic
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder

@CompileStatic
class DefaultHttpClientLifeCycle implements HttpClientLifeCycle {

	CloseableHttpClient createHttpClient() {
        def hourInMillis = 1000 * 60 * 60
        RequestConfig config = RequestConfig.custom().setSocketTimeout(hourInMillis).build()
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build()
    }

    void closeHttpClient(CloseableHttpClient client) {
        client.close()
    }
}