package com.clyze.client.web.http

import groovy.transform.CompileStatic
import org.apache.http.impl.client.CloseableHttpClient

@CompileStatic
class SameInstanceHttpClientLifeCycle implements HttpClientLifeCycle {
	
	private final CloseableHttpClient client

	SameInstanceHttpClientLifeCycle(CloseableHttpClient client) {
		this.client = client
	}

	CloseableHttpClient createHttpClient() {
		return client
	}

    void closeHttpClient(CloseableHttpClient client) {
    	//do nothing (keep-alive?)
    }
}