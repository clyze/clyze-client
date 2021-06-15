package com.clyze.client.web.api

import com.clyze.client.web.HttpDeleteWithBody
import com.clyze.client.web.PostState
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import java.nio.charset.StandardCharsets
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair

@CompileStatic
class LowLevelAPI {

    static final class InputConstants {
        public static final String ANALYSIS = "ANALYSIS"
    }

    static final String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    static final class Requests {

        static final HttpGet ping(String host, int port) {
            return new Endpoints(host, port).pingEndpoint()
        }

        static final HttpGet diagnose(String host, int port) {
            return new Endpoints(host, port).diagnoseEndpoint()
        }

        static final HttpPost cleanDeploy(String host, int port) {
            return new Endpoints(host, port).cleanDeployEndpoint()
        }

        static final HttpPost login(String username, String password, String host, int port) {
            HttpPost post = new Endpoints(host, port).authenticateEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        /*
        static final HttpDelete logout(String host, int port) {
            HttpDelete delete = new HttpDelete(createUrl(host, port, AUTH_PATH, "/session"))            
            if (userToken) delete.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)            
            return delete
        } 
        */

        static final HttpGet listStacks(String host, int port) {
            return new Endpoints(host, port).getStacksEndpoint()
        }

        static final HttpPost createAnalysis(String userToken, String snapshotId, String analysis, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(InputConstants.ANALYSIS, new StringBody(analysis))
            return createAnalysis(userToken, snapshotId, entityBuilder, host, port)
        }

        static final HttpPost createAnalysis(String userToken, String snapshotId, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new HttpPost(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/snapshots/${snapshotId}/analyses"))
            if (userToken) post.addHeader(Endpoints.HEADER_TOKEN, userToken)
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPut executeAnalysisAction(String userToken, String snapshotId, String analysis, String action, String host, int port) {
            HttpPut put = new HttpPut(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/snapshots/${snapshotId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return put
        }

        static final HttpGet getAnalysisStatus(String userToken, String snapshotId, String analysis, String host, int port) {
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/snapshots/${snapshotId}/analyses/${analysis}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }

        static final HttpGet getSymbolAt(String userToken, String snapshotId, String analysisId, String file, int line, int col, String host, int port) {
            String fileEncoded = encodeValue(file)
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/snapshots/${snapshotId}/symbols/${fileEncoded}/${line}/${col}?analysis=${analysisId}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }        

        static final HttpGet getUsers(String userToken, String host, int port) {
            return new Endpoints(host, port, userToken).listUsersEndpoint()
        }

        static final HttpPost createUser(String userToken, String username, String password, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken).postUserEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpDelete deleteUser(String userToken, String username, String host, int port) {
            new Endpoints(host, port, userToken).deleteUserEndpoint()
        }
    }

    static final class Projects {

        static final HttpGet getProjects(String userToken, String user, String host, int port) {
            return new Endpoints(host, port, userToken, user).listProjectsEndpoint()
        }

        static final HttpPost createProject(String userToken, String owner, String projectName, String[] stacks, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner).postProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("name", projectName))
            stacks.each {String st -> params.add(new BasicNameValuePair("stack", st)) }
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpGet getProject(String userToken, String owner, String name, String host, int port) {
            return new Endpoints(host, port, userToken, owner, name).getProjectEndpoint()
        }

        static final HttpPut updateProject(String userToken, String owner, String name, List<String> newMembers, String host, int port) {
            HttpPut put = new Endpoints(host, port, userToken, owner, name).putProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            newMembers?.each {
                params.add(new BasicNameValuePair("members", it))
            }
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpDelete deleteProject(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).deleteProjectEndpoint()
        }

        static final HttpPost createSampleProject(String userToken, String owner, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner).createSampleProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(0)
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPost repackageSnapshotForCI(String userToken, String owner, String projectName,
                                                     PostState postState, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName).repackageSnapshotForCIEndpoint()
            post.setEntity(postState.asMultipart().build())
            return post
        }

        static final HttpOptions getProjectOptions(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).createProjectOptionsEndpoint()
        }

        static final HttpGet getProjectAnalyses(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).getProjectAnalysesEndpoint()
        }
    }

    static final class Snapshots {

        static final HttpGet listSnapshots(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).listSnapshotsEndpoint()
        }

        static final HttpGet getSnapshot(String userToken, String owner, String projectName, String snapshotName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName).getSnapshotEndpoint()
        }

        static final HttpOptions getSnapshotOptions(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).getSnapshotOptionsEndpoint()
        }

        static final HttpGet getSymbol(String userToken, String owner, String projectName, String snapshotName, String symbolId, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName).getSymbolEndpoint(symbolId)
        }

        static final HttpPost createSnapshot(String userToken, String owner, String projectName,
                                             PostState postState, String host, int port) {
            MultipartEntityBuilder entityBuilder = postState.asMultipart()
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName).postSnapshotEndpoint()
            post.setEntity(entityBuilder.build())
            return post
        }

//        static final HttpPost createSnapshot(String userToken, String owner, String projectName, List<SnapshotInput> inputs,
//                                             String snapshotResolvableByServer, String host, int port) {
//            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
//            entityBuilder.addPart(InputConstants.INPUTS, new StringBody(snapshotResolvableByServer))
//            inputs.each { it.addTo(entityBuilder) }
//            return createSnapshot(userToken, owner, projectName, inputs, entityBuilder, host, port)
//        }
//
        static final HttpGet listSamples(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).listSamplesEndpoint()
        }

        static final HttpPost createSnapshotFromSample(String userToken, String owner, String projectName, String sampleName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).createSnapshotFromSampleEndpoint(sampleName)
        }

        static final HttpDelete deleteSnapshot(String userToken, String owner, String projectName, String snapshotName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName).deleteSnapshotEndpoint()
        }

        static final HttpGet listConfigurations(String userToken, String owner, String projectName, String snapshotName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName).listConfigurationsEndpoint()
        }

        static final HttpGet getConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).getConfigurationEndpoint()
        }

        static final HttpDelete deleteConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).deleteConfigurationEndpoint()
        }

        static final HttpPost cloneConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).cloneConfigurationEndpoint()
        }

        static final HttpPost pasteConfigurationRules(String userToken, String owner, String projectName, String snapshotName,
                                                      String config, String fromConfig, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).pasteConfigurationRulesEndpoint()
            List<NameValuePair> params = new ArrayList<>()
            params.add(new BasicNameValuePair('name', fromConfig))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPut updateConfiguration(String userToken, String owner, String projectName, String snapshotName,
                                                 String config, List<Tuple2<String, Object>> settings, String host, int port) {
            HttpPut put = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).updateConfigurationEndpoint()
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
                                                 String config, String newName, String host, int port) {
            HttpPut put = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).renameConfigurationEndpoint()
            List<NameValuePair> params = new ArrayList<>(1)
            params.add(new BasicNameValuePair('name', newName))
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpGet getRules(String userToken, String owner, String projectName, String snapshotName, String config, String originType, Integer start, Integer count, String host, int port) {
            Map<String, Object> extraParams = [
                    originType: originType,
                    _start     : start,
                    _count     : count
                    ] as Map<String, Object>
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config, extraParams).getRulesEndpoint()
        }

        static final HttpDeleteWithBody deleteRules(String userToken, String owner, String projectName, String snapshotName, String config, Collection<String> ids, String host, int port) {
            HttpDeleteWithBody delete = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).deleteRulesEndpoint()
            List<NameValuePair> params = new LinkedList<>()
            ids.forEach { String id -> params.add(new BasicNameValuePair('ids', id)) }
            delete.setEntity(new UrlEncodedFormEntity(params))
            return delete
        }

        static final HttpPost postRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleBody, String doopId, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).postRuleEndpoint()
            List<NameValuePair> params = new ArrayList<>(3)
            if (ruleBody)
                params.add(new BasicNameValuePair('ruleBody', ruleBody))
            if (doopId)
                params.add(new BasicNameValuePair('doopId', doopId))
            if (params.size() > 0)
                post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPut putRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleId, String ruleBody, String comment, String host, int port) {
            Map<String, Object> extraParams = [ruleId: ruleId] as Map<String, Object>
            HttpPut put = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config, extraParams).putRuleEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            if (ruleBody)
                params.add(new BasicNameValuePair('ruleBody', ruleBody))
            if (comment)
                params.add(new BasicNameValuePair('comment', comment))
            if (params.size() > 0)
                put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpDelete deleteRule(String userToken, String owner, String projectName, String snapshotName, String config, String ruleId, String host, int port) {
            Map<String, Object> extraParams = [ruleId: ruleId] as Map<String, Object>
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config, extraParams).deleteRuleEndpoint()
        }

        static final HttpGet exportConfiguration(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).exportConfigurationEndpoint()
        }

        static final HttpGet getRuntime(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).getRuntimeEndpoint()
        }

        static final HttpGet getOutput(String userToken, String owner, String projectName, String snapshotName, String config, String output, String host, int port) {
            Map<String, Object> extraParams = [output: output] as Map<String, Object>
            return new Endpoints(host, port, userToken, owner, projectName, snapshotName, config, extraParams).getOutputEndpoint()
        }

        static final HttpPost analyze(String userToken, String owner, String projectName, String snapshotName, String config, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName, snapshotName, config).analyzeEndpoint()
            // Use empty multipart
            post.setEntity(MultipartEntityBuilder.create().build())
            return post
        }
    }

    static final class Responses {

        static final def parseJson(HttpEntity entity) {
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return json
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

        public static final String BASE_PATH    = "/clue"
        public static final String API_PATH     = "${BASE_PATH}/api/v1"
        public static final String AUTH_PATH    = "${BASE_PATH}/auth/v1"
        public static final String HEADER_TOKEN = "x-clue-token"

        String host
        int port
        String userToken
        String username
        String projectName
        String snapshotName
        String config
        Map<String, Object> extraParams

        Endpoints(String host, int port, String userToken=null, String username=null,
                  String projectName=null, String snapshotName=null, String config=null,
                  Map<String, Object> extraParams=null) {
            this.host        = host
            this.port        = port
            this.userToken   = userToken
            this.username    = username
            this.projectName = projectName
            this.snapshotName= snapshotName
            this.config      = config
            this.extraParams = extraParams
        }

        HttpGet pingEndpoint() {
            return new HttpGet(createUrl(host, port, API_PATH, "/ping"))
        }

        HttpGet diagnoseEndpoint() {
            return new HttpGet(createUrl(host, port, BASE_PATH, "/diagnose"))
        }

        HttpPost cleanDeployEndpoint() {
            return new HttpPost(createUrl(host, port, BASE_PATH, "/clean/deploy"))
        }

        HttpPost authenticateEndpoint() {
            new HttpPost(createUrl(host, port, API_PATH, "/session"))
        }

        HttpGet listUsersEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, usersSuffix())))
        }

        HttpPost postUserEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, usersSuffix())))
        }

        HttpDelete deleteUserEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, userSuffix())))
        }

        HttpGet listProjectsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, projectsSuffix())))
        }

        HttpGet getStacksEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, "/options/stacks")))
        }

        HttpPost postProjectEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, projectsSuffix())))
        }

        HttpDelete deleteProjectEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, projectSuffix())))
        }

        HttpPost createSampleProjectEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, userSuffix() + "/phonograph")))
        }

        HttpGet getProjectEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, projectSuffix())))
        }

        HttpPut putProjectEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, projectSuffix())))
        }

        HttpPost repackageSnapshotForCIEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, projectSuffix() + "/repackage")))
        }

        HttpGet listSnapshotsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotsSuffix())))
        }

        HttpOptions createProjectOptionsEndpoint() {
            withTokenHeader(new HttpOptions(createUrl(host, port, API_PATH, snapshotsSuffix())))
        }

        HttpGet getProjectAnalysesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, analysesSuffix())))
        }

        HttpGet getSnapshotEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotSuffix())))
        }

        HttpOptions getSnapshotOptionsEndpoint() {
            withTokenHeader(new HttpOptions(createUrl(host, port, API_PATH, snapshotsSuffix())))
        }

        HttpGet getSymbolEndpoint(String symbolId) {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotSuffix() + '/symbols/byId/' + encodeValue(symbolId))))
        }

        HttpPost postSnapshotEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, snapshotsSuffix())))
        }

        HttpDelete deleteSnapshotEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, snapshotSuffix())))
        }

        HttpGet listSamplesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, samplesSuffix())))
        }

        HttpGet getConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotConfigSuffix())))
        }

        HttpDelete deleteConfigurationEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, snapshotConfigSuffix())))
        }

        HttpPut updateConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, snapshotConfigSuffix())))
        }

        HttpPut renameConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, snapshotConfigSuffix() + '/name')))
        }

        HttpPost cloneConfigurationEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, snapshotConfigSuffix() + '/clone')))
        }

        HttpPost pasteConfigurationRulesEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, snapshotConfigSuffix() + '/pasteRules')))
        }

        HttpGet getRulesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, rulesSuffix())))
        }

        HttpPost postRuleEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, rulesSuffix())))
        }

        HttpPut putRuleEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, ruleSuffix())))
        }

        HttpDelete deleteRuleEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, ruleSuffix())))
        }

        HttpDeleteWithBody deleteRulesEndpoint() {
            withTokenHeader(new HttpDeleteWithBody(createUrl(host, port, API_PATH, rulesSuffix())))
        }

        HttpGet exportConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotConfigSuffix() + '/export')))
        }

        HttpGet getRuntimeEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotConfigSuffix() + "/analysis/runtime")))
        }

        HttpGet listConfigurationsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, snapshotConfigsSuffix())))
        }

        HttpPost analyzeEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, snapshotConfigSuffix() + "/analyze")))
        }

        HttpGet getOutputEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, outputSuffix())))
        }

        HttpPost createSnapshotFromSampleEndpoint(String sampleName) {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, samplesSuffix() + "?name=${sampleName}")))
        }

        private <T extends HttpRequestBase> T withTokenHeader(T req) {
            if (userToken) req.addHeader(HEADER_TOKEN, userToken)
            return req
        }

        private static String usersSuffix() {
            return "/users"
        }

        String userSuffix() {
            if (!username) throw new RuntimeException("No username")
            return "${usersSuffix()}/$username"
        }

        String projectsSuffix() {
            return "${userSuffix()}/projects"
        }

        String projectSuffix() {
            if (!projectName) throw new RuntimeException("No projectName")
            return "${projectsSuffix()}/$projectName"
        }

        String snapshotsSuffix() {
            return "${projectSuffix()}/snapshots"
        }

        String snapshotSuffix() {
            if (!snapshotName) throw new RuntimeException("No snapshot name")
            return "${snapshotsSuffix()}/$snapshotName"
        }

        String snapshotConfigsSuffix() {
            return "${snapshotSuffix()}/configs"
        }

        String analysesSuffix() {
            return "${projectSuffix()}/analyses"
        }

        String snapshotConfigSuffix() {
            if (!config) throw new RuntimeException("No config")
            return "${snapshotConfigsSuffix()}/${config}"
        }

        String outputSuffix() {
            String output = extraParams['output'] as String
            if (!output) throw new RuntimeException("No output")
            return "${snapshotConfigSuffix()}/outputs/${output}"
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

        String rulesSuffix() {
            return "${snapshotConfigSuffix()}/rules" + getQueryForExtraParams()
        }

        String ruleSuffix() {
            String ruleId = extraParams['ruleId'] as String
            if (!ruleId) throw new RuntimeException("No rule id")
            return "${snapshotConfigSuffix()}/rules/${ruleId}"
        }

        String samplesSuffix() {
            return "${projectSuffix()}/samples"
        }

        static final String createUrl(String host, int port, String path, String endPoint) {
            return "http://${host}:${port}${path}${endPoint}" as String
        }
    }
}
