package org.clyze.client.web.http

import org.apache.http.impl.client.CloseableHttpClient

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