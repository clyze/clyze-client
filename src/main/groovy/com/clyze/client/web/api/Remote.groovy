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

	private final String hostPrefix
	private final HttpClientLifeCycle httpClientLifeCycle
	private String token    = null
	private String username = null

	private Remote(String hostPrefix, HttpClientLifeCycle httpClientLifeCycle) {
		this.hostPrefix = hostPrefix
		this.httpClientLifeCycle = httpClientLifeCycle
	}

	static Remote at(String hostPrefix, String user, String token) {
		CloseableHttpClient client = new DefaultHttpClientLifeCycle().createHttpClient()
		Remote r = new Remote(hostPrefix, new SameInstanceHttpClientLifeCycle(client))
		r.setUsername(user)
		r.setToken(token)
		return r
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
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				LowLevelAPI.Requests.ping(hostPrefix)
			}
		}.execute(hostPrefix)
	}

	Map<String, Object> diagnose() throws HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.diagnose(hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cleanDeploy(String user, String userToken) {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.cleanDeploy(hostPrefix, user, userToken)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listStacks() {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.listStacks(hostPrefix)
			}
		}.execute(hostPrefix)
	}

	Map<String, Object> login(String username, String password) throws HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.login(username, password, hostPrefix)
			}

			@Override Map<String, Object> onSuccess(HttpEntity entity) {
				Map<String, Object> data = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
				setToken(data.get('token') as String)
				setUsername(data.get('username') as String)
				return data
			}
		}.execute(hostPrefix)
	}

//	void logout() {
//		new HttpClientCommand(
//			httpClientLifeCycle: httpClientLifeCycle,
//			requestBuilder: LowLevelAPI.Requests.logout,
//			onSuccess: { HttpEntity entity ->
//				token = null
//			}
//		).execute(hostPrefix)
//	}

	@SuppressWarnings('unused')
	Map<String, Object> listSnapshots(String owner, String projectName)  {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.listSnapshots(token, owner, projectName, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createSnapshot(String owner, String projectName, PostState ps)
			throws ClientProtocolException, HttpHostConnectException {
		new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.createSnapshot(token, owner, projectName, ps, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSnapshot(String owner, String projectName, String snapshotName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getSnapshot(token, owner, projectName, snapshotName, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSymbol(String owner, String projectName, String snapshotName, String symbolId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getSymbol(token, owner, projectName, snapshotName, symbolId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getFiles(String owner, String projectName, String snapshotName, String artifact) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getFiles(token, owner, projectName, snapshotName, artifact, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getFile(String owner, String projectName, String snapshotName, String artifact, String file) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getFile(token, owner, projectName, snapshotName, artifact, file, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getCodeFile(String owner, String projectName, String snapshotName, String codeFile) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getCodeFile(token, owner, projectName, snapshotName, codeFile, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getCodeFileHints(String owner, String projectName, String snapshotName,
										 String config, String codeFile) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getCodeFileHints(token, owner, projectName, snapshotName, config, codeFile, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getAnalysisOutputFile(String owner, String projectName, String snapshotName,
											  String config, String analysisId, String codeFile) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getAnalysisOutputFile(token, owner, projectName, snapshotName, config, analysisId, codeFile, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteSnapshot(String owner, String projectName, String snapshotName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.deleteSnapshot(token, owner, projectName, snapshotName, hostPrefix)
			}
		}.execute(hostPrefix)
	}
	@SuppressWarnings('unused')
	Map<String, Object> listConfigurations(String owner, String projectName, String snapshotName)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.listConfigurations(token, owner, projectName, snapshotName, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getConfiguration(token, owner, projectName, snapshotName, config, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> cloneConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.cloneConfiguration(token, owner, projectName, snapshotName, config, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> renameConfiguration(String owner, String projectName, String snapshotName, String config, String newName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.renameConfiguration(token, owner, projectName, snapshotName, config, newName, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getRules(String owner, String projectName, String snapshotName, String config, String originType, Integer start, Integer count) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getRules(token, owner, projectName, snapshotName, config, originType, start, count, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> postRule(String owner, String projectName, String snapshotName, String config, String ruleBody, String doopId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.postRule(token, owner, projectName, snapshotName, config, ruleBody, doopId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> putRule(String owner, String projectName, String snapshotName, String config, String ruleId, String ruleBody, String comment) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.putRule(token, owner, projectName, snapshotName, config, ruleId, ruleBody, comment, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRule(String owner, String projectName, String snapshotName, String config, String ruleId) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.deleteRule(token, owner, projectName, snapshotName, config, ruleId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteRules(String owner, String projectName, String snapshotName, String config, Collection<String> ids) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.deleteRules(token, owner, projectName, snapshotName, config, ids, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> pasteConfigurationRules(String owner, String projectName, String snapshotName, String config, String fromConfig) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.pasteConfigurationRules(token, owner, projectName, snapshotName, config, fromConfig, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> updateConfiguration(String owner, String projectName, String snapshotName, String config,
											List<Tuple2<String, Object>> settings) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.updateConfiguration(token, owner, projectName, snapshotName, config, settings, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.deleteConfiguration(token, owner, projectName, snapshotName, config, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	String exportConfiguration(String owner, String projectName, String snapshotName, String config) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.exportConfiguration(token, owner, projectName, snapshotName, config, hostPrefix)
			}
		}.execute(hostPrefix) as String
	}

	@SuppressWarnings('unused')
	Map<String, Object> analyze(String owner, String projectName, String snapshotName, String config,
								String profileId, List<String> options)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.analyze(token, owner, projectName, snapshotName, config, profileId, options, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteAnalysis(String owner, String projectName, String snapshotName, String config,
									   String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.deleteAnalysis(token, owner, projectName, snapshotName, config, analysisId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getAnalysis(String owner, String projectName, String snapshotName, String config,
									String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getAnalysis(token, owner, projectName, snapshotName, config, analysisId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getAnalysisRuntime(String owner, String projectName, String snapshotName, String config,
										   String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getAnalysisRuntime(token, owner, projectName, snapshotName, config, analysisId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> executeAnalysisAction(String owner, String projectName, String snapshotName, String config,
											  String action, String analysisId)  {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.executeAnalysisAction(token, owner, projectName, snapshotName, config, action, analysisId, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	String repackageSnapshotForCI(String owner, String projectName, PostState ps, AttachmentHandler<String> handler) throws ClientProtocolException {
		return new HttpStringClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.repackageSnapshotForCI(token, owner, projectName, ps, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	boolean executeAnalysisAction(String snapshotId, String analysisId, String action) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.executeAnalysisAction(token, snapshotId, analysisId, action, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getSymbolAt(String snapshotId, String analysisId, String file, int line, int col) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.getSymbolAt(token, snapshotId, analysisId, file, line, col, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listUsers() throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.listUsers(token, username, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	Map<String, Object> createUser(String newUsername, String newPassword) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.createUser(token, newUsername, newPassword, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	Map<String, Object> deleteUser(String username) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Requests.deleteUser(token, username, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listPublicProjects() {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.getPublicProjects(token, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> listProjects(String owner) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.getProjects(token, owner, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> createProject(String owner, String name, List<String> stacks, String isPublic) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.createProject(token, owner, name, stacks, isPublic, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProject(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.getProject(token, owner, name, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProjectInputs(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override
			HttpUriRequest buildRequest(String hostPrefix) {
				println "getProjectInputs.owner=${owner}/token=${token}"
				return LowLevelAPI.Projects.getProjectInputs(token, owner, name, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getProjectAnalyses(String owner, String name) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.getProjectAnalyses(token, owner, name, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> deleteProject(String owner, String projectName) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.deleteProject(token, owner, projectName, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	@SuppressWarnings('unused')
	Map<String, Object> getOutput(String owner, String name, String snapshotName, String config, String analysisId, String output, String start, String count, String appOnly) throws ClientProtocolException {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Snapshots.getOutput(token, owner, name, snapshotName, config, analysisId, output, start, count, appOnly, hostPrefix)
			}
		}.execute(hostPrefix)
	}

	Map<String, Object> updateProject(String owner, String name, List<String> newMembers) {
		return new HttpMapClientCommand(httpClientLifeCycle) {
			@Override HttpUriRequest buildRequest(String hostPrefix) {
				return LowLevelAPI.Projects.updateProject(token, owner, name, newMembers, hostPrefix)
			}
		}.execute(hostPrefix)
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
