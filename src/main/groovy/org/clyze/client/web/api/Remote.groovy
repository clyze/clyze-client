package org.clyze.client.web.api

import groovy.transform.CompileStatic
import org.clyze.client.web.PostState
import org.clyze.client.web.http.*
import org.apache.http.HttpEntity
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.impl.client.CloseableHttpClient

@CompileStatic
class Remote {

	private final String host
	private final Integer port
	private final HttpClientLifeCycle httpClientLifeCycle	
	private String token    = null
	private String username = null

	private Remote(String host, Integer port, HttpClientLifeCycle httpClientLifeCycle) {
		this.host = host
		this.port = port
		this.httpClientLifeCycle = httpClientLifeCycle
	}

	static Remote at(String host, Integer port) {
		CloseableHttpClient client = new DefaultHttpClientLifeCycle().createHttpClient()
		return new Remote(host, port, new SameInstanceHttpClientLifeCycle(client))
	}

	String currentUser() {
		return username
	}

	@SuppressWarnings('unused')
	boolean isLoggedIn() {
		return (token != null)
	}

	@SuppressWarnings('unused')
	Map<String, Object> ping() throws HttpHostConnectException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&ping,
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	Map<String, Object> diagnose() throws HttpHostConnectException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&diagnose,
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port) as Map<String, Object>
	}

	@SuppressWarnings('unused')
	Map<String, Object> cleanDeploy() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&cleanDeploy,
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> login(String username, String password) throws HttpHostConnectException {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&login.curry(username, password),
			onSuccess: { HttpEntity entity ->
				Map<String, Object> data = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				this.token    = data.get('token')
				this.username = data.get('username')
				return data
			}
		).execute(host, port)		
	}

//	void logout() {
//		new HttpClientCommand(
//			httpClientLifeCycle: httpClientLifeCycle,
//			requestBuilder: LowLevelAPI.Requests.&logout,
//			onSuccess: { HttpEntity entity ->
//				token = null
//			}
//		).execute(host, port)
//	}

	@SuppressWarnings('unused')
	Map<String, Object> listBuilds(String owner, String projectName)  {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Builds.&listBuilds.curry(token, owner, projectName),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> createBuild(String owner, String projectName, String profile, PostState ps) throws HttpHostConnectException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Builds.&createBuild.curry(token, owner, projectName, profile, ps.asMultipart()),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> listSamples(String owner, String projectName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&listSamples.curry(token, owner, projectName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createBuildFromSample(String owner, String projectName, String sampleName) throws HttpHostConnectException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&createBuildFromSample.curry(token, owner, projectName, sampleName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getBuild(String owner, String projectName, String buildName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&getBuild.curry(token, owner, projectName, buildName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteBuild(String owner, String projectName, String buildName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&deleteBuild.curry(token, owner, projectName, buildName),
				onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}
	@SuppressWarnings('unused')
	Map<String, Object> listConfigurations(String owner, String projectName, String buildName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&listConfigurations.curry(token, owner, projectName, buildName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getConfiguration(String owner, String projectName, String buildName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&getConfiguration.curry(token, owner, projectName, buildName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cloneConfiguration(String owner, String projectName, String buildName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&cloneConfiguration.curry(token, owner, projectName, buildName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteConfiguration(String owner, String projectName, String buildName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&deleteConfiguration.curry(token, owner, projectName, buildName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	String exportConfiguration(String owner, String projectName, String buildName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&exportConfiguration.curry(token, owner, projectName, buildName, config),
				onSuccess: LowLevelAPI.Responses.&asString
		).execute(host, port) as String
	}

	@SuppressWarnings('unused')
	Map<String, Object> analyze(String owner, String projectName, String buildName, String config, String profile)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&analyze.curry(token, owner, projectName, buildName, config, profile),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	def <T> T repackageBuildForCI(String owner, String projectName, PostState ps, AttachmentHandler<T> handler) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&repackageBuildForCI.curry(token, owner, projectName, ps.asMultipart()),
				onSuccess: handler.&handleAttachment
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	String createAnalysis(String buildId, String analysis) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, buildId, analysis),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	String createAnalysis(String buildId, PostState ps) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, buildId, ps.asMultipart()),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	boolean executeAnalysisAction(String buildId, String analysisId, String action) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&executeAnalysisAction.curry(token, buildId, analysisId, action),
			onSuccess: { HttpEntity entity ->
            	LowLevelAPI.Responses.parseJson(entity) != null
        	}
		).execute(host, port)				
	}

	String getAnalysisStatus(String buildId, String analysisId) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getAnalysisStatus.curry(token, buildId, analysisId),
			onSuccess : { HttpEntity entity ->
				Map<String, Object> json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				return (json.get('analysis') as Map<String, Object>).get('state')
			}
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	String waitForAnalysisStatus(Set<String> statusSet, String buildId, String analysisId, int minutes) {

		long millis = minutes * 60 * 1000
		long expireAt = System.currentTimeMillis() + millis

		def state = null		
		
		while(!Thread.currentThread().isInterrupted() && !statusSet.contains(state) && System.currentTimeMillis() < expireAt) {
			try {
				state = getAnalysisStatus(buildId, analysisId)
				Thread.sleep(15000)
			}
			catch(InterruptedException ie) {
				throw new RuntimeException("Analysis wait interrupted: ${ie.message}")
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

	@SuppressWarnings('unused')
	Map<String, Object> getSymbolAt(String buildId, String analysisId, String file, int line, int col) {
		String fileEncoded = URLEncoder.encode(file, "UTF-8")		
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getSymbolAt.curry(token, buildId, analysisId, file, line, col),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> listUsers() throws ClientProtocolException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getUsers.curry(token),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	Map<String, Object> createUser(String username, String password) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createUser.curry(token, username, password),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	Map<String, Object> deleteUser(String username) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&deleteUser.curry(token, username),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> listProjects(String owner) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&getProjects.curry(token, owner),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> createProject(String owner, String name, String platform) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&createProject.curry(token, owner, name, platform),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProject(String owner, String name) throws ClientProtocolException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&getProject.curry(token, owner, name),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteProject(String owner, String projectName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&deleteProject.curry(token, owner, projectName),
				onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	def getOutput(String owner, String name, String buildName, String config, String output) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Builds.&getOutput.curry(token, owner, name, buildName, config, output),
				onSuccess : LowLevelAPI.Responses.&asString
		).execute(host, port)
	}

	Map<String, Object> updateProject(String owner, String name, List<String> newMembers) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&updateProject.curry(token, owner, name, newMembers),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> createSampleProject(String owner) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&createSampleProject.curry(token, owner),
				onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}
}
