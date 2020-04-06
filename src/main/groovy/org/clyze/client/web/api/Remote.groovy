package org.clyze.client.web.api

//import groovy.transform.CompileStatic
import org.clyze.client.web.PostState
import org.clyze.client.web.http.*
import org.apache.http.HttpEntity
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.impl.client.CloseableHttpClient

//@CompileStatic
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

	boolean isLoggedIn() {
		return (token != null)
	}

	def ping() throws HttpHostConnectException {
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

	def cleanDeploy() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&cleanDeploy,
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}		

	def login(String username, String password) throws HttpHostConnectException {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&login.curry(username, password),
			onSuccess: { HttpEntity entity ->
				def data = LowLevelAPI.Responses.parseJson(entity)
				this.token    = data.token
				this.username = data.username
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

	def listBundles(String owner, String projectName)  {
		new HttpClientCommand(			
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Bundles.&listBundles.curry(token, owner, projectName),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def createBundle(String owner, String projectName, String profile, PostState ps) throws HttpHostConnectException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Bundles.&createBundle.curry(token, owner, projectName, profile, ps.asMultipart()),
			onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def listSamples(String owner, String projectName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&listSamples.curry(token, owner, projectName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def createBundleFromSample(String owner, String projectName, String sampleName) throws HttpHostConnectException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&createBundleFromSample.curry(token, owner, projectName, sampleName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def getBundle(String owner, String projectName, String bundleName) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&getBundle.curry(token, owner, projectName, bundleName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def listConfigurations(String owner, String projectName, String bundleName)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&listConfigurations.curry(token, owner, projectName, bundleName),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def getConfiguration(String owner, String projectName, String bundleName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&getConfiguration.curry(token, owner, projectName, bundleName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def exportConfiguration(String owner, String projectName, String bundleName, String config) {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&exportConfiguration.curry(token, owner, projectName, bundleName, config),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def analyze(String owner, String projectName, String bundleName, String config, String profile)  {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Bundles.&analyze.curry(token, owner, projectName, bundleName, config, profile),
				onSuccess: LowLevelAPI.Responses.&parseJson
		).execute(host, port)
	}

	def <T> T repackageBundleForCI(String owner, String projectName, PostState ps, AttachmentHandler<T> handler) throws ClientProtocolException {
		new HttpClientCommand(
				httpClientLifeCycle: httpClientLifeCycle,
				requestBuilder: LowLevelAPI.Projects.&repackageBundleForCI.curry(token, owner, projectName, ps.asMultipart()),
				onSuccess: handler.&handleAttachment
		).execute(host, port)
	}

	String createAnalysis(String bundleId, String analysis) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, bundleId, analysis),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	String createAnalysis(String bundleId, PostState ps) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createAnalysis.curry(token, bundleId, ps.asMultipart()),
			onSuccess: LowLevelAPI.Responses.&parseJsonAndGetAttr.curry("id")
		).execute(host, port)		
	}

	boolean executeAnalysisAction(String bundleId, String analysisId, String action) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&executeAnalysisAction.curry(token, bundleId, analysisId, action),
			onSuccess: { HttpEntity entity ->
            	LowLevelAPI.Responses.parseJson(entity) != null
        	}
		).execute(host, port)				
	}

	String getAnalysisStatus(String bundleId, String analysisId) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getAnalysisStatus.curry(token, bundleId, analysisId),
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

	def getSymbolAt(String bundleId, String analysisId, String file, int line, int col) {		
		String fileEncoded = URLEncoder.encode(file, "UTF-8")		
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getSymbolAt.curry(token, bundleId, analysisId, file, line, col),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}		

	def listUsers() throws ClientProtocolException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&getUsers.curry(token),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def createUser(String username, String password) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&createUser.curry(token, username, password),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def deleteUser(String username) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Requests.&deleteUser.curry(token, username),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def listProjects() {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&getProjects.curry(token, currentUser()),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def createProject(String name) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&createProject.curry(token, currentUser(), name),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def getProject(String owner, String name) throws ClientProtocolException {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&getProject.curry(token, owner, name),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}

	def updateProject(String owner, String name, List<String> newMembers) {
		new HttpClientCommand(
			httpClientLifeCycle: httpClientLifeCycle,
			requestBuilder: LowLevelAPI.Projects.&updateProject.curry(token, owner, name, newMembers),
			onSuccess : LowLevelAPI.Responses.&parseJson
		).execute(host, port)		
	}
}
