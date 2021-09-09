package com.clyze.client.web.api

import com.clyze.client.web.HttpDeleteWithBody
import com.clyze.client.web.PostState
import groovy.transform.CompileStatic
import java.nio.charset.StandardCharsets
import org.apache.http.HttpEntity
import org.apache.http.HttpHeaders
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import org.clyze.persistent.metadata.JSONUtil

@CompileStatic
class LowLevelAPI {

    static final String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    static final class Requests {

        static final HttpGet ping(String hostPrefix) {
            return new Endpoints(hostPrefix, null).pingEndpoint()
        }

        static final HttpGet diagnose(String hostPrefix) {
            return new Endpoints(hostPrefix, null).diagnoseEndpoint()
        }

        static final HttpPost cleanDeploy(String hostPrefix, String username, String userToken) {
            return new Endpoints(hostPrefix, userToken, username).cleanDeployEndpoint()
        }

        static final HttpPost login(String username, String password, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, null).loginEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpGet listStacks(String hostPrefix) {
            return new Endpoints(hostPrefix, null).getStacksEndpoint()
        }

        static final HttpPut executeAnalysisAction(String userToken, String snapshotId, String analysis, String action, String hostPrefix) {
            HttpPut put = new HttpPut(Endpoints.createUrl(hostPrefix, Endpoints.API_PATH, "/snapshots/${snapshotId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return put
        }

        static final HttpGet getSymbolAt(String userToken, String snapshotId, String analysisId, String file, int line, int col, String hostPrefix) {
            String fileEncoded = encodeValue(file)
            HttpGet get = new HttpGet(Endpoints.createUrl(hostPrefix, Endpoints.API_PATH, "/snapshots/${snapshotId}/symbols/${fileEncoded}/${line}/${col}?analysis=${analysisId}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }        

        static final HttpGet listUsers(String userToken, String username, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, username).listUsersEndpoint()
        }

        static final HttpPost createUser(String userToken, String username, String password,
                                         String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken).postUserEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpDelete deleteUser(String userToken, String username, String hostPrefix) {
            new Endpoints(hostPrefix, userToken).deleteUserEndpoint()
        }
    }

    static final class Projects {

        static final HttpGet getPublicProjects(String userToken, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken).listPublicProjectsEndpoint()
        }

        static final HttpGet getProjects(String userToken, String user, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, user).listProjectsEndpoint()
        }

        static final HttpPost createProject(String userToken, String owner, String projectName, List<String> stacks,
                                            String isPublic, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner).postProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair('name', projectName))
            stacks.each {String st -> params.add(new BasicNameValuePair("stack", st)) }
            if (isPublic)
                params.add(new BasicNameValuePair('isPublic', isPublic))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpGet getProject(String userToken, String owner, String name, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, name).getProjectEndpoint()
        }

        static final HttpPut updateProject(String userToken, String owner, String name, List<String> newMembers, String hostPrefix) {
            HttpPut put = new Endpoints(hostPrefix, userToken, owner, name).putProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            newMembers?.each {
                params.add(new BasicNameValuePair("members", it))
            }
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpDelete deleteProject(String userToken, String owner, String projectName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName).deleteProjectEndpoint()
        }

        static final HttpPost repackageSnapshotForCI(String userToken, String owner, String projectName,
                                                     PostState postState, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner, projectName).repackageSnapshotForCIEndpoint()
            post.setEntity(postState.asMultipart().build())
            return post
        }

        static final HttpGet getProjectAnalyses(String userToken, String owner, String projectName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName).getProjectAnalysesEndpoint()
        }

        static final HttpGet getProjectInputs(String userToken, String owner, String projectName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName).getProjectInputsEndpoint()
        }
    }

    static final class Snapshots {

        static final HttpGet listSnapshots(String userToken, String owner, String projectName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName).listSnapshotsEndpoint()
        }

        static final HttpGet getSnapshot(String userToken, String owner, String projectName, String snapshotName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).getSnapshotEndpoint()
        }

        static final HttpGet getSymbol(String userToken, String owner, String projectName, String snapshotName, String symbolId, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).getSymbolEndpoint(symbolId)
        }

        static final HttpGet getFile(String userToken, String owner, String projectName, String snapshotName,
                                     String artifact, String file, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).getFileEndpoint(artifact, file)
        }

        static final HttpGet getFiles(String userToken, String owner, String projectName, String snapshotName,
                                      String artifact, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).getFilesEndpoint(artifact)
        }

        static final HttpGet getCodeFile(String userToken, String owner, String projectName, String snapshotName,
                                         String codeFile, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).getCodeFileEndpoint(codeFile)
        }

        static final HttpGet getCodeFileHints(String userToken, String owner, String projectName, String snapshotName,
                                              String config, String codeFile, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getCodeFileHintsEndpoint(codeFile)
        }

        static final HttpGet getAnalysisOutputFile(String userToken, String owner, String projectName, String snapshotName,
                                                   String config, String analysisId, String codeFile, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getAnalysisOutputFileEndpoint(analysisId, codeFile)
        }

        static final HttpPost createSnapshot(String userToken, String owner, String projectName,
                                             PostState postState, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner, projectName).postSnapshotEndpoint()
            post.setEntity(postState.asMultipart().build())
            return post
        }

//        static final HttpPost createSnapshot(String userToken, String owner, String projectName, List<SnapshotInput> inputs,
//                                             String snapshotResolvableByServer, String hostPrefix) {
//            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
//            entityBuilder.addPart(InputConstants.INPUTS, new StringBody(snapshotResolvableByServer))
//            inputs.each { it.addTo(entityBuilder) }
//            return createSnapshot(userToken, owner, projectName, inputs, entityBuilder, host, port)
//        }
//

        static final HttpDelete deleteSnapshot(String userToken, String owner, String projectName, String snapshotName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).deleteSnapshotEndpoint()
        }

        static final HttpGet listConfigurations(String userToken, String owner, String projectName, String snapshotName, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName).listConfigurationsEndpoint()
        }

        static final HttpGet getConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getConfigurationEndpoint()
        }

        static final HttpDelete deleteConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).deleteConfigurationEndpoint()
        }

        static final HttpPost cloneConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).cloneConfigurationEndpoint()
        }

        static final HttpPost pasteConfigurationRules(String userToken, String owner, String projectName, String snapshotName,
                                                      String config, String fromConfig, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).pasteConfigurationRulesEndpoint()
            List<NameValuePair> params = new ArrayList<>()
            params.add(new BasicNameValuePair('name', fromConfig))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPut updateConfiguration(String userToken, String owner, String projectName, String snapshotName,
                                                 String config, List<Tuple2<String, Object>> settings, String hostPrefix) {
            HttpPut put = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).updateConfigurationEndpoint()
            List<NameValuePair> params = new ArrayList<>()
            settings?.each {
                def value = it.v2
                String value1
                if (value instanceof Boolean)
                    value1 = value ? "on" : "off"
                else if (value instanceof String)
                    value1 = value
                else {
                    value1 = value.toString()
                    println "Unhandled form data element: ${value1} of type ${value.class}"
                }
                params.add(new BasicNameValuePair(it.v1, value1))
            }
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpPut renameConfiguration(String userToken, String owner, String projectName, String snapshotName,
                                                 String config, String newName, String hostPrefix) {
            HttpPut put = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).renameConfigurationEndpoint()
            List<NameValuePair> params = new ArrayList<>(1)
            params.add(new BasicNameValuePair('name', newName))
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpGet getRules(String userToken, String owner, String projectName, String snapshotName, String config, String originType, Integer start, Integer count, String hostPrefix) {
            Map<String, Object> extraParams = [
                    originType: originType,
                    _start     : start,
                    _count     : count
                    ] as Map<String, Object>
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config, extraParams).getRulesEndpoint()
        }

        static final HttpDeleteWithBody deleteRules(String userToken, String owner, String projectName, String snapshotName, String config, Collection<String> ids, String hostPrefix) {
            HttpDeleteWithBody delete = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).deleteRulesEndpoint()
            List<NameValuePair> params = new LinkedList<>()
            ids.forEach { String id -> params.add(new BasicNameValuePair('ids', id)) }
            delete.setEntity(new UrlEncodedFormEntity(params))
            return delete
        }

        static final HttpPost postRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleBody, String doopId, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).postRuleEndpoint()
            List<NameValuePair> params = new ArrayList<>(3)
            if (ruleBody)
                params.add(new BasicNameValuePair('ruleBody', ruleBody))
            if (doopId)
                params.add(new BasicNameValuePair('doopId', doopId))
            if (params.size() > 0)
                post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPut putRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleId, String ruleBody, String comment, String hostPrefix) {
            Map<String, Object> extraParams = [ruleId: ruleId] as Map<String, Object>
            HttpPut put = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config, extraParams).putRuleEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            if (ruleBody)
                params.add(new BasicNameValuePair('ruleBody', ruleBody))
            if (comment)
                params.add(new BasicNameValuePair('comment', comment))
            if (params.size() > 0)
                put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpDelete deleteRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleId, String hostPrefix) {
            Map<String, Object> extraParams = [ruleId: ruleId] as Map<String, Object>
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config, extraParams).deleteRuleEndpoint()
        }

        static final HttpGet exportConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).exportConfigurationEndpoint()
        }

        static final HttpGet getRuntime(String userToken, String owner, String projectName, String snapshotName, String config, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getRuntimeEndpoint()
        }

        static final HttpGet getOutput(String userToken, String owner, String projectName, String snapshotName,
                                       String config, String analysisId, String output, String start, String count,
                                       String appOnly, String hostPrefix) {
            Map<String, Object> extraParams = [analysis: analysisId, output: output] as Map<String, Object>
            if (start != null)
                extraParams.put('_start', start)
            if (count != null)
                extraParams.put('_count', count)
            if (appOnly != null)
                extraParams.put('appOnly', appOnly)
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config, extraParams).getOutputEndpoint()
        }

        static final HttpPost analyze(String userToken, String owner, String projectName, String snapshotName,
                                      String config, String profileId, List<String> options, String hostPrefix) {
            HttpPost post = new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).analyzeEndpoint(profileId)
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
            for (String option : options) {
                int eqIdx = option.indexOf('=')
                if (eqIdx > 0)
                    entityBuilder.addPart(option.substring(0, eqIdx), new StringBody(option.substring(eqIdx + 1)))
                else
                    System.err.println "WARNING: ignoring bad option ${option}"
            }
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpGet getAnalysis(String userToken, String owner, String projectName, String snapshotName,
                                         String config, String analysisId, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getAnalysisEndpoint(analysisId)
        }

        static final HttpDelete deleteAnalysis(String userToken, String owner, String projectName, String snapshotName,
                                               String config, String analysisId, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).deleteAnalysisEndpoint(analysisId)
        }

        static final HttpPut executeAnalysisAction(String userToken, String owner, String projectName,
                                                   String snapshotName, String config, String action,
                                                   String analysisId, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config)
                    .executeAnalysisActionEndpoint(action, analysisId)
        }

        static final HttpGet getAnalysisRuntime(String userToken, String owner, String projectName, String snapshotName,
                                                String config, String analysisId, String hostPrefix) {
            return new Endpoints(hostPrefix, userToken, owner, projectName, snapshotName, config).getAnalysisRuntimeEndpoint(analysisId)
        }

    }

    static final class Responses {

        static final def parseJson(HttpEntity entity) {
            return JSONUtil.toMap(asString(entity))
        }

        static final def parseJsonAndGetAttr(HttpEntity entity, String attrName) {
            def json = parseJson(entity)
            return json[(attrName)]
        }

        static final String asString(HttpEntity entity) {
            entity.getContent().text
        }
    }

    private static final class Endpoints {

        public static final String API_PATH     = "/api/v1"
        public static final String HEADER_TOKEN = "x-clue-token"

        String hostPrefix
        String userToken
        String username
        String projectName
        String snapshotName
        String config
        Map<String, Object> extraParams

        Endpoints(String hostPrefix, String userToken, String username=null,
                  String projectName=null, String snapshotName=null, String config=null,
                  Map<String, Object> extraParams=null) {
            this.hostPrefix  = hostPrefix
            this.userToken   = userToken
            this.username    = username
            this.projectName = projectName
            this.snapshotName= snapshotName
            this.config      = config
            this.extraParams = extraParams
        }

        HttpGet pingEndpoint() {
            return new HttpGet(createUrl(hostPrefix, API_PATH, '/ping'))
        }

        HttpGet diagnoseEndpoint() {
            return new HttpGet(createUrl(hostPrefix, '', '/diagnose'))
        }

        HttpPost cleanDeployEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, '', '/clean/deploy')))
        }

        HttpPost loginEndpoint() {
            new HttpPost(createUrl(hostPrefix, API_PATH, "/login"))
        }

        HttpGet listUsersEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, usersPrefix())))
        }

        HttpPost postUserEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, usersPrefix())))
        }

        HttpDelete deleteUserEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, userPrefix())))
        }

        HttpGet listProjectsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, projectsPrefix())))
        }

        HttpGet listPublicProjectsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, '/public-projects')))
        }

        HttpGet getStacksEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, "/options/stacks")))
        }

        HttpPost postProjectEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, projectsPrefix())))
        }

        HttpDelete deleteProjectEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, projectPrefix())))
        }

        HttpGet getProjectEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, projectPrefix())))
        }

        HttpPut putProjectEndpoint() {
            withTokenHeader(new HttpPut(createUrl(hostPrefix, API_PATH, projectPrefix())))
        }

        HttpPost repackageSnapshotForCIEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, projectPrefix() + "/repackage")))
        }

        HttpGet listSnapshotsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotsPrefix())))
        }

        HttpGet getProjectInputsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, "${projectPrefix()}/snapshots-inputs")))
        }

        HttpGet getProjectAnalysesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, analysesPrefix())))
        }

        HttpGet getSnapshotEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotPrefix())))
        }

        HttpGet getSymbolEndpoint(String symbolId) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotPrefix() + '/symbols/byId/' + encodeValue(symbolId))))
        }

        String filesPrefix(String artifact) {
            return snapshotPrefix() + '/artifacts/' + encodeValue(artifact) + '/files'
        }

        HttpGet getFilesEndpoint(String artifact) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, filesPrefix(artifact) + '/')))
        }

        HttpGet getFileEndpoint(String artifact, String file) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, filesPrefix(artifact) + '/' + encodeValue(file))))
        }

        HttpGet getCodeFileEndpoint(String codeFile) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotPrefix() + '/code/' + encodeValue(codeFile))))
        }

        HttpGet getCodeFileHintsEndpoint(String codeFile) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/code/' + encodeValue(codeFile) + '/hints')))
        }

        HttpGet getAnalysisOutputFileEndpoint(String analysisId, String file) {
            String opts = "analysis=${encodeValue(analysisId)}&file=${encodeValue(file)}"
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/analysis/output-file?' + opts)))
        }

        HttpPost postSnapshotEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, snapshotsPrefix())))
        }

        HttpDelete deleteSnapshotEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, snapshotPrefix())))
        }

        HttpGet getConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix())))
        }

        HttpDelete deleteConfigurationEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix())))
        }

        HttpPut updateConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix())))
        }

        HttpPut renameConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/name')))
        }

        HttpPost cloneConfigurationEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/clone')))
        }

        HttpPost pasteConfigurationRulesEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/pasteRules')))
        }

        HttpGet getRulesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, rulesPrefix())))
        }

        HttpPost postRuleEndpoint() {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, rulesPrefix())))
        }

        HttpPut putRuleEndpoint() {
            withTokenHeader(new HttpPut(createUrl(hostPrefix, API_PATH, rulePrefix())))
        }

        HttpDelete deleteRuleEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, rulePrefix())))
        }

        HttpDeleteWithBody deleteRulesEndpoint() {
            withTokenHeader(new HttpDeleteWithBody(createUrl(hostPrefix, API_PATH, rulesPrefix())))
        }

        HttpGet exportConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + '/export')))
        }

        HttpGet getRuntimeEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + "/analysis/runtime")))
        }

        HttpGet listConfigurationsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigsPrefix())))
        }

        HttpPost analyzeEndpoint(String profileId) {
            withTokenHeader(new HttpPost(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + "/analyze?profile=" + encodeValue(profileId))))
        }

        String analysisPrefix(String analysisId) {
            return snapshotConfigPrefix() + "/analysis?analysis=" + encodeValue(analysisId)
        }

        HttpGet getAnalysisEndpoint(String analysisId) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, analysisPrefix(analysisId))))
        }

        HttpGet getAnalysisRuntimeEndpoint(String analysisId) {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, snapshotConfigPrefix() + "/analysis/runtime?analysis=" + encodeValue(analysisId))))
        }

        HttpDelete deleteAnalysisEndpoint(String analysisId) {
            withTokenHeader(new HttpDelete(createUrl(hostPrefix, API_PATH, analysisPrefix(analysisId))))
        }

        String analysisActionPrefix(String action, String analysisId) {
            return snapshotConfigPrefix() + "/analysis/action/${action}?analysis=" + encodeValue(analysisId)
        }

        HttpPut executeAnalysisActionEndpoint(String action, String analysisId) {
            withTokenHeader(new HttpPut(createUrl(hostPrefix, API_PATH, analysisActionPrefix(action, analysisId))))
        }

        HttpGet getOutputEndpoint() {
            withTokenHeader(new HttpGet(createUrl(hostPrefix, API_PATH, outputPrefix())))
        }

        private <T extends HttpRequestBase> T withTokenHeader(T req) {
            if (userToken) {
                req.addHeader(HEADER_TOKEN, userToken)
                req.setHeader(HttpHeaders.AUTHORIZATION, 'Bearer ' + userToken)
            }
            return req
        }

        private static String usersPrefix() {
            return "/users"
        }

        String userPrefix() {
            if (!username) throw new RuntimeException("No username")
            return "${usersPrefix()}/${username}"
        }

        String projectsPrefix() {
            return "${userPrefix()}/projects"
        }

        String projectPrefix() {
            if (!projectName) throw new RuntimeException("No projectName")
            return "${projectsPrefix()}/${projectName}"
        }

        String snapshotsPrefix() {
            return "${projectPrefix()}/snapshots"
        }

        String snapshotPrefix() {
            if (!snapshotName) throw new RuntimeException("No snapshot name")
            return "${snapshotsPrefix()}/${snapshotName}"
        }

        String snapshotConfigsPrefix() {
            return "${snapshotPrefix()}/configs"
        }

        String analysesPrefix() {
            return "${projectPrefix()}/analyses"
        }

        String snapshotConfigPrefix() {
            if (!config) throw new RuntimeException("No config")
            return "${snapshotConfigsPrefix()}/${config}"
        }

        String outputPrefix() {
            String analysisId = extraParams['analysis'] as String
            if (!analysisId) throw new RuntimeException("No analysis id")
            String output = extraParams['output'] as String
            if (!output) throw new RuntimeException("No output")
            String ret = "${snapshotConfigPrefix()}/analysis/outputs?analysis=${encodeValue(analysisId)}&dataset=${encodeValue(output)}"
            for (String key : ['_start', '_count', 'appOnly']) {
                String value = extraParams[key] as String
                if (value != null)
                    ret += '&' + key + '=' + encodeValue(value)
            }
            return ret
        }

        String getQueryForExtraParams() {
            // Unpack additional parameters.
            List<String> q = new LinkedList<>()
            if (extraParams && extraParams.size() > 0) {
                extraParams.forEach { k, v ->
                    if (v)
                        q.add(k + '=' + encodeValue(v.toString())) }
            }
            return q.size() == 0 ? '' : '?' + q.join('&')
        }

        String rulesPrefix() {
            return "${snapshotConfigPrefix()}/rules" + getQueryForExtraParams()
        }

        String rulePrefix() {
            String ruleId = extraParams['ruleId'] as String
            if (!ruleId) throw new RuntimeException("No rule id")
            return "${snapshotConfigPrefix()}/rules/${ruleId}"
        }

        static final String createUrl(String hostPrefix, String path, String endPoint) {
            return "http://${hostPrefix}${path}${endPoint}" as String
        }
    }
}
