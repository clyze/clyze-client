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
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&ping,
			onSuccess: onSuccess
		).execute(host, port)		
	}

	
	public <T> T cleanDeploy(Closure<T> onSuccess) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&cleanDeploy,
			onSuccess: onSuccess
		).execute(host, port)		
	}	

	public void login(String username, String password) {		
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&login.curry(username, password),
			onSuccess: { HttpEntity entity ->
				userToken = LowLevelAPI.Responses.parseJsonAndGetAttr("token", entity)
			}
		).execute(host, port)		
	}

	public boolean isLoggedIn() {
		return (userToken != null)
	}

	public <T> T listBundles(Closure<T> onSuccess)  {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&listBundles.curry(userToken),
			onSuccess: onSuccess
		).execute(host, port)		
	}

	public String createDoopBundle(String platform, String bundleResolvableByServer) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createDoopBundle.curry(userToken, platform, bundleResolvableByServer),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	public String createAnalysis(String bundleId, String analysis) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(userToken, bundleId, analysis),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	boolean executeAnalysisAction(String bundleId, String analysisId, String action) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&executeAnalysisAction.curry(userToken, bundleId, analysisId, action),
			onSuccess: { HttpEntity entity ->
            	LowLevelAPI.Responses.parseJson(entity) != null
        	}
		).execute(host, port)				
	}
}