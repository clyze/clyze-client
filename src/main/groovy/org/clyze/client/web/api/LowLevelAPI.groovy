package org.clyze.client.web.api

import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair

import groovy.transform.CompileStatic
import org.clyze.client.web.HttpDeleteWithBody

@CompileStatic
class LowLevelAPI {


    static final class InputConstants {
        public static final String INPUTS   = "INPUTS"
        public static final String PLATFORM = "PLATFORM"
        public static final String ANALYSIS = "ANALYSIS"
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


        static final HttpPost createAnalysis(String userToken, String buildId, String analysis, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(InputConstants.ANALYSIS, new StringBody(analysis))
            return createAnalysis(userToken, buildId, entityBuilder, host, port)
        }

        static final HttpPost createAnalysis(String userToken, String buildId, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new HttpPost(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${buildId}/analyses"))
            if (userToken) post.addHeader(Endpoints.HEADER_TOKEN, userToken)
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPut executeAnalysisAction(String userToken, String buildId, String analysis, String action, String host, int port) {
            HttpPut put = new HttpPut(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${buildId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return put
        }

        static final HttpGet getAnalysisStatus(String userToken, String buildId, String analysis, String host, int port) {
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${buildId}/analyses/${analysis}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }

        static final HttpGet getSymbolAt(String userToken, String buildId, String analysisId, String file, int line, int col, String host, int port) {
            String fileEncoded = URLEncoder.encode(file, "UTF-8")       
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${buildId}/symbols/${fileEncoded}/${line}/${col}?analysis=${analysisId}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }        

        static final HttpGet getProfileOptions(String host, int port) {
            return new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/options/bundles"))
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

        static final HttpPost createProject(String userToken, String owner, String projectName, String platform, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner).postProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("name", projectName))
            params.add(new BasicNameValuePair("platform", platform))
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

        static final HttpPost repackageBuildForCI(String userToken, String owner, String projectName, String profile,
                                                  MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName).repackageBuildForCIEndpoint(profile)
            post.setEntity(entityBuilder.build())
            return post
        }
    }

    static final class Builds {

        static final HttpGet listBuilds(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).listBuildsEndpoint()
        }

        static final HttpGet getBuild(String userToken, String owner, String projectName, String buildName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName).getBuildEndpoint()
        }

        static final HttpPost createBuild(String userToken, String owner, String projectName, String profile, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName).postBuildEndpoint(profile)
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPost createBuild(String userToken, String owner, String projectName, String profile, String buildResolvableByServer, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
            entityBuilder.addPart(InputConstants.INPUTS, new StringBody(buildResolvableByServer))
            entityBuilder.addPart(InputConstants.PLATFORM, new StringBody(profile))
            return createBuild(userToken, owner, projectName, profile, entityBuilder, host, port)
        }

        static final HttpGet listSamples(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).listSamplesEndpoint()
        }

        static final HttpPost createBuildFromSample(String userToken, String owner, String projectName, String sampleName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).createBuildFromSampleEndpoint(sampleName)
        }

        static final HttpDelete deleteBuild(String userToken, String owner, String projectName, String buildName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName).deleteBuildEndpoint()
        }

        static final HttpGet listConfigurations(String userToken, String owner, String projectName, String buildName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName).listConfigurationsEndpoint()
        }

        static final HttpGet getConfiguration(String userToken, String owner, String projectName, String buildName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config).getConfigurationEndpoint()
        }

        static final HttpDelete deleteConfiguration(String userToken, String owner, String projectName, String buildName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config).deleteConfigurationEndpoint()
        }

        static final HttpPost cloneConfiguration(String userToken, String owner, String projectName, String buildName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config).cloneConfigurationEndpoint()
        }

        static final HttpPost pasteConfigurationRules(String userToken, String owner, String projectName, String buildName,
                                                      String config, String fromConfig, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName, buildName, config).pasteConfigurationRulesEndpoint()
            List<NameValuePair> params = new ArrayList<>()
            params.add(new BasicNameValuePair('name', fromConfig))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpPut updateConfiguration(String userToken, String owner, String projectName, String buildName,
                                                 String config, List<Tuple2<String, Object>> settings, String host, int port) {
            HttpPut put = new Endpoints(host, port, userToken, owner, projectName, buildName, config).updateConfigurationEndpoint()
            List<NameValuePair> params = new ArrayList<>()
            settings?.each {
                def value = it.second
                String value1
                if (value instanceof Boolean)
                    value1 = value ? "on" : "off"
                else if (value instanceof String)
                    value1 = value
                else {
                    value1 = value.toString()
                    println "Unhandled form data element: ${value1} of type ${value.class}"
                }
                params.add(new BasicNameValuePair(it.first, value1))
            }
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpPut renameConfiguration(String userToken, String owner, String projectName, String buildName,
                                                 String config, String newName, String host, int port) {
            HttpPut put = new Endpoints(host, port, userToken, owner, projectName, buildName, config).renameConfigurationEndpoint()
            List<NameValuePair> params = new ArrayList<>(1)
            params.add(new BasicNameValuePair('name', newName))
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
        }

        static final HttpGet getRules(String userToken, String owner, String projectName, String buildName, String config, String originType, Integer start, Integer count, String host, int port) {
            List<Object> extraParams = [originType, start, count] as List<Object>
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config, extraParams).getRulesEndpoint()
        }

        static final HttpDeleteWithBody deleteRules(String userToken, String owner, String projectName, String buildName, String config, Collection<String> ids, String host, int port) {
            HttpDeleteWithBody delete = new Endpoints(host, port, userToken, owner, projectName, buildName, config).deleteRulesEndpoint()
            List<NameValuePair> params = new LinkedList<>()
            ids.forEach { String id -> params.add(new BasicNameValuePair('ids', id)) }
            delete.setEntity(new UrlEncodedFormEntity(params))
            return delete
        }

        static final HttpGet exportConfiguration(String userToken, String owner, String projectName, String buildName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config).exportConfigurationEndpoint()
        }

        static final HttpGet getRuntime(String userToken, String owner, String projectName, String buildName, String config, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config).getRuntimeEndpoint()
        }

        static final HttpGet getOutput(String userToken, String owner, String projectName, String buildName, String config, String output, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, buildName, config, [output] as List<Object>).getOutputEndpoint()
        }

        static final HttpPost analyze(String userToken, String owner, String projectName, String buildName, String config, String profile, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName, buildName, config).analyzeEndpoint(profile)
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
        String buildName
        String config
        List<?> extraParams

        Endpoints(String host, int port, String userToken=null, String username=null,
                  String projectName=null, String buildName=null, String config=null,
                  List<?> extraParams=null) {
            this.host        = host
            this.port        = port
            this.userToken   = userToken
            this.username    = username
            this.projectName = projectName
            this.buildName   = buildName
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
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, usersSuffix()))) as HttpGet
        }

        HttpPost postUserEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, usersSuffix()))) as HttpPost
        }

        HttpDelete deleteUserEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, userSuffix()))) as HttpDelete
        }

        HttpGet listProjectsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, projectsSuffix()))) as HttpGet
        }

        HttpPost postProjectEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, projectsSuffix()))) as HttpPost
        }

        HttpDelete deleteProjectEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, projectSuffix()))) as HttpDelete
        }

        HttpPost createSampleProjectEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, userSuffix() + "/phonograph"))) as HttpPost
        }

        HttpGet getProjectEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, projectSuffix()))) as HttpGet
        }

        HttpPut putProjectEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, projectSuffix()))) as HttpPut
        }

        HttpPost repackageBuildForCIEndpoint(String profile) {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, projectSuffix() + "/repackage?profile=${profile}"))) as HttpPost
        }

        HttpGet listBuildsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildsSuffix()))) as HttpGet
        }

        HttpGet getBuildEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildSuffix()))) as HttpGet
        }

        HttpPost postBuildEndpoint(String profile) {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, buildsSuffix() + "?profile=${profile}"))) as HttpPost
        }

        HttpDelete deleteBuildEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, buildSuffix()))) as HttpDelete
        }

        HttpGet listSamplesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, samplesSuffix()))) as HttpGet
        }

        HttpGet getConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildConfigSuffix()))) as HttpGet
        }

        HttpDelete deleteConfigurationEndpoint() {
            withTokenHeader(new HttpDelete(createUrl(host, port, API_PATH, buildConfigSuffix()))) as HttpDelete
        }

        HttpPut updateConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, buildConfigSuffix()))) as HttpPut
        }

        HttpPut renameConfigurationEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, buildConfigSuffix() + '/name'))) as HttpPut
        }

        HttpPost cloneConfigurationEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, buildConfigSuffix() + '/clone'))) as HttpPost
        }

        HttpPost pasteConfigurationRulesEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, buildConfigSuffix() + '/pasteRules'))) as HttpPost
        }

        HttpGet getRulesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, rulesSuffix()))) as HttpGet
        }

        HttpDeleteWithBody deleteRulesEndpoint() {
            withTokenHeader(new HttpDeleteWithBody(createUrl(host, port, API_PATH, rulesSuffix()))) as HttpDeleteWithBody
        }

        HttpGet exportConfigurationEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildConfigSuffix() + '/export'))) as HttpGet
        }

        HttpGet getRuntimeEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildConfigSuffix() + "/analysis/runtime"))) as HttpGet
        }

        HttpGet listConfigurationsEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, buildConfigsSuffix()))) as HttpGet
        }

        HttpPost analyzeEndpoint(String profile) {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, buildConfigSuffix() + "/analyze?profile=${profile}"))) as HttpPost
        }

        HttpGet getOutputEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, outputSuffix()))) as HttpGet
        }

        HttpPost createBuildFromSampleEndpoint(String sampleName) {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, samplesSuffix() + "?name=${sampleName}"))) as HttpPost
        }

        private HttpRequestBase withTokenHeader(HttpRequestBase req) {
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

        String buildsSuffix() {
            return "${projectSuffix()}/bundles"
        }

        String buildSuffix() {
            if (!buildName) throw new RuntimeException("No build name")
            return "${buildsSuffix()}/$buildName"
        }

        String buildConfigsSuffix() {
            return "${buildSuffix()}/configs"
        }

        String buildConfigSuffix() {
            if (!config) throw new RuntimeException("No config")
            return "${buildConfigsSuffix()}/${config}"
        }

        String outputSuffix() {
            String output = extraParams.get(0) as String
            if (!output) throw new RuntimeException("No output")
            return "${buildConfigSuffix()}/outputs/${output}"
        }

        String rulesSuffix() {
            // Unpack additional parameters.
            String query = ''
            if (extraParams && extraParams.size() > 0) {
                List<String> q = new LinkedList<>()
                String originType = extraParams.get(0) as String
                if (originType != null)
                    q.add('originType=' + originType)
                Integer start = extraParams.get(1) as Integer
                if (start != null)
                    q.add('_start=' + start)
                Integer count = extraParams.get(2) as Integer
                if (count != null)
                    q.add('_count=' + count)

                query = q.size() == 0 ? '' : '?' + q.join('&')
            }
            return "${buildConfigSuffix()}/rules" + query
        }

        String samplesSuffix() {
            return "${projectSuffix()}/samples"
        }

        static final String createUrl(String host, int port, String path, String endPoint) {
            return "http://${host}:${port}${path}${endPoint}" as String
        }
    }
}
