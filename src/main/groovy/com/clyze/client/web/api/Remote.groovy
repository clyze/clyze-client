package com.clyze.client.web.api

import com.clyze.client.web.PostState
import com.clyze.client.web.http.DefaultHttpClientLifeCycle
import com.clyze.client.web.http.HttpClientLifeCycle
import com.clyze.client.web.http.HttpMapClientCommand
import com.clyze.client.web.http.HttpStringClientCommand
import com.clyze.client.web.http.SameInstanceHttpClientLifeCycle
import groovy.transform.CompileStatic
import org.apache.http.HttpEntity
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpUriRequest
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
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				LowLevelAPI.Requests.ping(host, port)
			}
		}.execute(host, port)
	}

	Map<String, Object> diagnose() throws HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.diagnose(host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cleanDeploy() {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.cleanDeploy(host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listStacks() {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.listStacks(host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> login(String username, String password) throws HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.login(username, password, host, port)
			}

			@Override Map<String, Object> onSuccess(HttpEntity entity) {
				Map<String, Object> data = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				setToken(data.get('token') as String)
				setUsername(data.get('username') as String)
				return data
			}
		}.execute(host, port)
	}

//	void logout() {
//		new HttpClientCommand(
//			httpClientLifeCycle: httpClientLifeCycle,
//			requestBuilder: LowLevelAPI.Requests.logout,
//			onSuccess: { HttpEntity entity ->
//				token = null
//			}
//		).execute(host, port)
//	}

	@SuppressWarnings('unused')
	Map<String, Object> listSnapshots(String owner, String projectName)  {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.listSnapshots(token, owner, projectName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSnapshotOptions(String owner, String projectName)  {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getSnapshotOptions(token, owner, projectName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createSnapshot(String owner, String projectName, PostState ps)
			throws ClientProtocolException, HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.createSnapshot(token, owner, projectName, ps, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSnapshot(String owner, String projectName, String snapshotName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getSnapshot(token, owner, projectName, snapshotName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSymbol(String owner, String projectName, String snapshotName, String symbolId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getSymbol(token, owner, projectName, snapshotName, symbolId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getFiles(String owner, String projectName, String snapshotName, String artifact) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getFiles(token, owner, projectName, snapshotName, artifact, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getFile(String owner, String projectName, String snapshotName, String artifact, String file) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getFile(token, owner, projectName, snapshotName, artifact, file, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getCodeFile(String owner, String projectName, String snapshotName, String codeFile) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getCodeFile(token, owner, projectName, snapshotName, codeFile, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteSnapshot(String owner, String projectName, String snapshotName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.deleteSnapshot(token, owner, projectName, snapshotName, host, port)
			}
		}.execute(host, port)
	}
	@SuppressWarnings('unused')
	Map<String, Object> listConfigurations(String owner, String projectName, String snapshotName)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.listConfigurations(token, owner, projectName, snapshotName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getConfiguration(token, owner, projectName, snapshotName, config, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cloneConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.cloneConfiguration(token, owner, projectName, snapshotName, config, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> renameConfiguration(String owner, String projectName, String snapshotName, String config, String newName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.renameConfiguration(token, owner, projectName, snapshotName, config, newName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getRules(String owner, String projectName, String snapshotName, String config, String originType, Integer start, Integer count) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getRules(token, owner, projectName, snapshotName, config, originType, start, count, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> postRule(String owner, String projectName, String snapshotName, String config, String ruleBody, String doopId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.postRule(token, owner, projectName, snapshotName, config, ruleBody, doopId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> putRule(String owner, String projectName, String snapshotName, String config, String ruleId, String ruleBody, String comment) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.putRule(token, owner, projectName, snapshotName, config, ruleId, ruleBody, comment, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRule(String owner, String projectName, String snapshotName, String config, String ruleId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.deleteRule(token, owner, projectName, snapshotName, config, ruleId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRules(String owner, String projectName, String snapshotName, String config, Collection<String> ids) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.deleteRules(token, owner, projectName, snapshotName, config, ids, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> pasteConfigurationRules(String owner, String projectName, String snapshotName, String config, String fromConfig) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.pasteConfigurationRules(token, owner, projectName, snapshotName, config, fromConfig, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> updateConfiguration(String owner, String projectName, String snapshotName, String config,
											List<Tuple2<String, Object>> settings) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.updateConfiguration(token, owner, projectName, snapshotName, config, settings, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.deleteConfiguration(token, owner, projectName, snapshotName, config, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	String exportConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.exportConfiguration(token, owner, projectName, snapshotName, config, host, port)
			}
		}.execute(host, port) as String
	}

	@SuppressWarnings('unused')
	Map<String, Object> analyze(String owner, String projectName, String snapshotName, String config, String profileId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.analyze(token, owner, projectName, snapshotName, config, profileId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteAnalysis(String owner, String projectName, String snapshotName, String config,
									   String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.deleteAnalysis(token, owner, projectName, snapshotName, config, analysisId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getAnalysis(String owner, String projectName, String snapshotName, String config,
									String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getAnalysis(token, owner, projectName, snapshotName, config, analysisId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getAnalysisRuntime(String owner, String projectName, String snapshotName, String config,
										   String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getAnalysisRuntime(token, owner, projectName, snapshotName, config, analysisId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> executeAnalysisAction(String owner, String projectName, String snapshotName, String config,
											  String action, String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.executeAnalysisAction(token, owner, projectName, snapshotName, config, action, analysisId, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	String repackageSnapshotForCI(String owner, String projectName, PostState ps, AttachmentHandler<String> handler) throws ClientProtocolException {
		return new HttpStringClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.repackageSnapshotForCI(token, owner, projectName, ps, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	boolean executeAnalysisAction(String snapshotId, String analysisId, String action) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.executeAnalysisAction(token, snapshotId, analysisId, action, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSymbolAt(String snapshotId, String analysisId, String file, int line, int col) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.getSymbolAt(token, snapshotId, analysisId, file, line, col, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listUsers() throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.getUsers(token, host, port)
			}
		}.execute(host, port)
	}

	Map<String, Object> createUser(String username, String password) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.createUser(token, username, password, host, port)
			}
		}.execute(host, port)
	}

	Map<String, Object> deleteUser(String username) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Requests.deleteUser(token, username, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listProjects(String owner) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.getProjects(token, owner, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createProject(String owner, String name, String[] stacks) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.createProject(token, owner, name, stacks, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProject(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.getProject(token, owner, name, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProjectOptions(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override
			HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.getProjectOptions(token, owner, name, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProjectAnalyses(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.getProjectAnalyses(token, owner, name, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteProject(String owner, String projectName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.deleteProject(token, owner, projectName, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	def getOutput(String owner, String name, String snapshotName, String config, String analysisId, String output, String start, String count) throws ClientProtocolException {
		return new HttpStringClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Snapshots.getOutput(token, owner, name, snapshotName, config, analysisId, output, start, count, host, port)
			}
			@Override String onSuccess(HttpEntity entity) {
				return LowLevelAPI.Responses.asString(entity)
			}
		}.execute(host, port)
	}

	Map<String, Object> updateProject(String owner, String name, List<String> newMembers) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String host, int port) {
				return LowLevelAPI.Projects.updateProject(token, owner, name, newMembers, host, port)
			}
		}.execute(host, port)
	}

	@SuppressWarnings('unused')
	boolean waitForAnalysisStatus(String owner, String projectName, String buildName, String config,
								  String analysisId, Set<String> statusSet, int totalTries = 60) {
		println "Build ${buildName}: waiting for analysis ${analysisId} to finish..."
		try {
			for (int tries = 0; tries < totalTries; tries++) {
				println "Checking analysis state (${tries} of ${totalTries})..."
				String state = getAnalysisStatus(owner, projectName, buildName, config, analysisId)
				println "Current state: ${state}"
				if (statusSet.contains(state))
					return true
				Thread.sleep(5000)
			}
			println "Too many tries, analysis assumed to be stuck."
		} catch (Exception ex) {
			println "Error checking analysis state: ${ex.message}"
			ex.printStackTrace()
		}
		return false
	}

	String getAnalysisStatus(String owner, String projectName, String buildName,
							 String configName, String analysisId) {
		Map<String, Object> config = getConfiguration(owner, projectName, buildName, configName)
		String state = (config?.get('analyses') as List<Map<String, String>>)
				.find { Map<String, String> an -> an.get('id') == analysisId }?.get('state')
		return state
	}

}
