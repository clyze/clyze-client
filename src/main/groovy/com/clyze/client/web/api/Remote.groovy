package com.clyze.client.web.api

import com.clyze.client.web.PostState
import com.clyze.client.web.http.DefaultHttpClientLifeCycle
import com.clyze.client.web.http.HttpClientCommand
import com.clyze.client.web.http.HttpClientLifeCycle
import com.clyze.client.web.http.SameInstanceHttpClientLifeCycle
import groovy.transform.CompileStatic
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

	void setUsername(String name) {
		this.username = name
	}

	void setToken(String t) {
		this.token = t
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
	Map<String, Object> listStacks() {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Requests.&listStacks,
				onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> login(String username, String password) throws HttpHostConnectException {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&login.curry(username, password),
			onSuccess: { HttpEntity entity ->
				Map<String, Object> data = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				setToken(data.get('token') as String)
				setUsername(data.get('username') as String)
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
	Map<String, Object> listSnapshots(String owner, String projectName)  {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Snapshots.&listSnapshots.curry(token, owner, projectName),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	Map<String, Object> createSnapshot(String owner, String projectName, PostState ps)
			throws ClientProtocolException, HttpHostConnectException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Snapshots.&createSnapshot.curry(token, owner, projectName, ps),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listSamples(String owner, String projectName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&listSamples.curry(token, owner, projectName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createSnapshotFromSample(String owner, String projectName, String sampleName) throws HttpHostConnectException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&createSnapshotFromSample.curry(token, owner, projectName, sampleName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSnapshot(String owner, String projectName, String snapshotName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&getSnapshot.curry(token, owner, projectName, snapshotName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteSnapshot(String owner, String projectName, String snapshotName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&deleteSnapshot.curry(token, owner, projectName, snapshotName),
				onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}
	@SuppressWarnings('unused')
	Map<String, Object> listConfigurations(String owner, String projectName, String snapshotName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&listConfigurations.curry(token, owner, projectName, snapshotName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getConfiguration(String owner, String projectName, String snapshotName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&getConfiguration.curry(token, owner, projectName, snapshotName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cloneConfiguration(String owner, String projectName, String snapshotName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&cloneConfiguration.curry(token, owner, projectName, snapshotName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> renameConfiguration(String owner, String projectName, String snapshotName, String config, String newName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&renameConfiguration.curry(token, owner, projectName, snapshotName, config, newName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getRules(String owner, String projectName, String snapshotName, String config, String originType, Integer start, Integer count) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&getRules.curry(token, owner, projectName, snapshotName, config, originType, start, count),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> postRule(String owner, String projectName, String snapshotName, String config, String ruleBody, String doopId) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&postRule.curry(token, owner, projectName, snapshotName, config, ruleBody, doopId),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> putRule(String owner, String projectName, String snapshotName, String config, String ruleId, String ruleBody, String comment) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&putRule.curry(token, owner, projectName, snapshotName, config, ruleId, ruleBody, comment),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRule(String owner, String projectName, String snapshotName, String config, String ruleId) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&deleteRule.curry(token, owner, projectName, snapshotName, config, ruleId),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRules(String owner, String projectName, String snapshotName, String config, Collection<String> ids) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&deleteRules.curry(token, owner, projectName, snapshotName, config, ids),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> pasteConfigurationRules(String owner, String projectName, String snapshotName, String config, String fromConfig) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&pasteConfigurationRules.curry(token, owner, projectName, snapshotName, config, fromConfig),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> updateConfiguration(String owner, String projectName, String snapshotName, String config,
											List<Tuple2<String, Object>> settings) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&updateConfiguration.curry(token, owner, projectName, snapshotName, config, settings),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteConfiguration(String owner, String projectName, String snapshotName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&deleteConfiguration.curry(token, owner, projectName, snapshotName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	String exportConfiguration(String owner, String projectName, String snapshotName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&exportConfiguration.curry(token, owner, projectName, snapshotName, config),
				onSuccess: LowLevelAPI.Responses.&asString
		).execute(host, port) as String
	}

	@SuppressWarnings('unused')
	Map<String, Object> analyze(String owner, String projectName, String snapshotName, String config)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&analyze.curry(token, owner, projectName, snapshotName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	<T> T repackageSnapshotForCI(String owner, String projectName, PostState ps, AttachmentHandler<T> handler) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&repackageSnapshotForCI.curry(token, owner, projectName, ps.asMultipart()),
				onSuccess: handler.&handleAttachment
		).execute(host, port)
	}

	@SuppressWarnings('unused')
	String createAnalysis(String snapshotId, String analysis) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, snapshotId, analysis),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	String createAnalysis(String snapshotId, PostState ps) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, snapshotId, ps.asMultipart()),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	boolean executeAnalysisAction(String snapshotId, String analysisId, String action) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&executeAnalysisAction.curry(token, snapshotId, analysisId, action),
			onSuccess: { HttpEntity entity ->
            	LowLevelAPI.Responses.parseJson(entity) != null
        	}
		).execute(host, port)				
	}

	String getAnalysisStatus(String snapshotId, String analysisId) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getAnalysisStatus.curry(token, snapshotId, analysisId),
			onSuccess : { HttpEntity entity ->
				Map<String, Object> json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				return (json.get('analysis') as Map<String, Object>).get('state')
			}
		).execute(host, port)		
	}

	@SuppressWarnings('unused')
	String waitForAnalysisStatus(Set<String> statusSet, String snapshotId, String analysisId, int minutes) {

		long millis = minutes * 60 * 1000
		long expireAt = System.currentTimeMillis() + millis

		def state = null		
		
		while(!Thread.currentThread().isInterrupted() && !statusSet.contains(state) && System.currentTimeMillis() < expireAt) {
			try {
				state = getAnalysisStatus(snapshotId, analysisId)
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
	Map<String, Object> getSymbolAt(String snapshotId, String analysisId, String file, int line, int col) {
		String fileEncoded = URLEncoder.encode(file, "UTF-8")		
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getSymbolAt.curry(token, snapshotId, analysisId, file, line, col),
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
	Map<String, Object> createProject(String owner, String name, String[] stacks) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&createProject.curry(token, owner, name, stacks),
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
	Map<String, Object> getProjectOptions(String owner, String name) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&getProjectOptions.curry(token, owner, name),
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
	def getOutput(String owner, String name, String snapshotName, String config, String output) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Snapshots.&getOutput.curry(token, owner, name, snapshotName, config, output),
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
