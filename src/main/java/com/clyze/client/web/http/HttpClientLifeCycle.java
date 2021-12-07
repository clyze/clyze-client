package com.clyze.client.web.http;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientLifeCycle {
    CloseableHttpClient createHttpClient();

    void closeHttpClient(CloseableHttpClient client);
}
