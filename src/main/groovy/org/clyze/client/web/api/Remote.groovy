package org.clyze.client.web.api

import org.clyze.client.web.http.*

import org.apache.http.impl.client.CloseableHttpClient

import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.client.ClientProtocolException
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
		CloseableHttpClient client = new DefaultHttpClientLifeCycle().createHttpClient()
		return new Remote(host, port, new SameInstanceHttpClientLifeCycle(client))
	}


	public <T> T ping() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&ping,
			onSuccess: { HttpEntity entity ->
				LowLevelAPI.Responses.parseJson(entity) != null
			}
		).execute(host, port)		
	}

	
	public <T> T cleanDeploy() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&cleanDeploy,
			onSuccess: { HttpEntity entity ->
				LowLevelAPI.Responses.parseJson(entity) != null
			}
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

	public def listBundles()  {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&listBundles.curry(userToken),
			onSuccess: LowLevelAPI.Responses.&parseJson
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

	String getAnalysisStatus(String bundleId, String analysisId) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getAnalysisStatus.curry(userToken, bundleId, analysisId),
			onSuccess : { HttpEntity entity ->
				def json = LowLevelAPI.Responses.parseJson(entity)
				return json.analysis.state
			}
		).execute(host, port)		
	}

	String waitForAnalysisStatus(Set<String> statusSet, String bundleId, String analysisId, int minutes) {

		long millis = minutes * 60 * 1000
		long expireAt = System.currentTimeMillis() + millis

		def state = null		
		
		while(!Thread.currentThread().isInterrupted() && !statusSet.contains(state) && System.currentTimeMillis() < expireAt) {
			try {
				state = getAnalysisStatus(bundleId, analysisId)			
				Thread.sleep(15000)
			}
			catch(InterruptedException ie) {
				throw new RuntimeException("Analysis wait interrupted")
			}
			catch(ClientProtocolException cpe) {
				throw new RuntimeException("Analysis wait server error - ${cpe.getMessage()}", cpe)
			}
			catch(Throwable other) {
				throw new RuntimeException("Analysis wait internal error - ${other.getMessage()}", other)
			}
		}

		state
	}

	def getSymbolAt(String bundleId, String analysisId, String file, int line, int col) {		
		String fileEncoded = URLEncoder.encode(file, "UTF-8")		
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getSymbolAt.curry(userToken, bundleId, analysisId, file, line, col),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}		

	def listUsers() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getUsers.curry(userToken),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def createUser(String username, String password) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createUser.curry(userToken, username, password),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def deleteUser(String username) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&deleteUser.curry(userToken, username),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}
}