package org.clyze.client.web.api

import org.clyze.client.web.http.*

import org.apache.http.impl.client.CloseableHttpClient

import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair

class Remote {

	private final String host
	private final Integer port
	private final HttpClientLifeCycle httpClientLifeCycle	
	private String userToken = null

	private Remote(String host, Integer port, HttpClientLifeCycle httpClientLifeCycle) {
		this.host = host
		this.port = port
		this.httpClientLifeCycle = httpClientLifeCycle
	}

	static Remote at(String host, Integer port) {
		final CloseableHttpClient client = new DefaultHttpClientLifeCycle().createHttpClient()
		return new Remote(host, port, new SameInstanceHttpClientLifeCycle(client))
	}


	public <T> T ping(Closure<T> onSuccess) {
		HttpClientCommand.extend(LowLevelAPI.PING, onSuccess:onSuccess, httpClientLifeCycle:httpClientLifeCycle).execute(host, port)
	}

	
	public <T> T cleanDeploy(Closure<T> onSuccess) {
		HttpClientCommand.extend(LowLevelAPI.CLEAN_DEPLOY, onSuccess:onSuccess, httpClientLifeCycle:httpClientLifeCycle).execute(host, port)
	}	

	public void login(String username, String password) {		
		new HttpClientCommand(			
			requestBuilder: { String host, int port ->
				HttpPost post = LowLevelAPI.LOGIN.requestBuilder.call(host, port)
				List<NameValuePair> params = new ArrayList<>(2)
	            params.add(new BasicNameValuePair("username", username))
	            params.add(new BasicNameValuePair("password", password))
	            post.setEntity(new UrlEncodedFormEntity(params))
	            return post
			},
			onSuccess: { HttpEntity entity ->
				userToken = LowLevelAPI.LOGIN.onSuccess.call(entity)
			}
		).execute(host, port)		
	}

	public boolean isLoggedIn() {
		return (userToken != null)
	}

	public <T> T listBundles(Closure<T> onSuccess)  {
		new HttpClientCommand(			
			requestBuilder: { String host, int port ->
				HttpGet get = LowLevelAPI.LIST_BUNDLES.requestBuilder.call(host, port)
				if (userToken) get.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)
			},
			onSuccess: onSuccess
		).execute(host, port)		
	}

}