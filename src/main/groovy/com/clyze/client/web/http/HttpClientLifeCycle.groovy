package com.clyze.client.web.http

import groovy.transform.CompileStatic
import org.apache.http.impl.client.CloseableHttpClient

@CompileStatic
interface HttpClientLifeCycle {
	CloseableHttpClient createHttpClient()
    void closeHttpClient(CloseableHttpClient client)
}
