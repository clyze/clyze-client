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


        static final HttpPost createAnalysis(String userToken, String bundleId, String analysis, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(InputConstants.ANALYSIS, new StringBody(analysis))
            return createAnalysis(userToken, bundleId, entityBuilder, host, port)
        }

        static final HttpPost createAnalysis(String userToken, String bundleId, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new HttpPost(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${bundleId}/analyses"))
            if (userToken) post.addHeader(Endpoints.HEADER_TOKEN, userToken)
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPut executeAnalysisAction(String userToken, String bundleId, String analysis, String action, String host, int port) {
            HttpPut put = new HttpPut(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${bundleId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return put
        }

        static final HttpGet getAnalysisStatus(String userToken, String bundleId, String analysis, String host, int port) {
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${bundleId}/analyses/${analysis}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }

        static final HttpGet getSymbolAt(String userToken, String bundleId, String analysisId, String file, int line, int col, String host, int port) {
            String fileEncoded = URLEncoder.encode(file, "UTF-8")       
            HttpGet get = new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/bundles/${bundleId}/symbols/${fileEncoded}/${line}/${col}?analysis=${analysisId}"))
            if (userToken) get.addHeader(Endpoints.HEADER_TOKEN, userToken)
            return get
        }        

        static final HttpGet getOptionsForCreate(String what, String host, int port) {
            return new HttpGet(Endpoints.createUrl(host, port, Endpoints.API_PATH, "/options?what=${what}"))
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

        static final HttpPost createProject(String userToken, String owner, String projectName, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner).postProjectEndpoint()
            List<NameValuePair> params = new ArrayList<>(1)
            params.add(new BasicNameValuePair("name", projectName))
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
    }

    static final class Bundles {

        static final HttpGet listBundles(String userToken, String owner, String projectName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName).listBundlesEndpoint()
        }

        static final HttpGet getBundle(String userToken, String owner, String projectName, String bundleName, String host, int port) {
            return new Endpoints(host, port, userToken, owner, projectName, bundleName).getBundleEndpoint()
        }

        static final HttpPost createDoopBundle(String userToken, String owner, String projectName, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new Endpoints(host, port, userToken, owner, projectName).postDoopBundleEndpoint()
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPost createDoopBundle(String userToken, String owner, String projectName, String platform, String bundleResolvableByServer, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
            entityBuilder.addPart(InputConstants.INPUTS, new StringBody(bundleResolvableByServer))
            entityBuilder.addPart(InputConstants.PLATFORM, new StringBody(platform))
            return createDoopBundle(userToken, owner, projectName, entityBuilder, host, port)
        }
    }

    static final class Responses {

        static final def parseJson(HttpEntity entity) {
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return json
        }

        static final def parseJsonAndGetAttr(String attrName, HttpEntity entity) {
            def json = parseJson(entity)
            return json[(attrName)]
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
        String bundleName

        Endpoints(String host, int port, String userToken=null, String username=null, String projectName=null, String bundleName=null) {
            this.host        = host
            this.port        = port
            this.userToken   = userToken
            this.username    = username
            this.projectName = projectName
            this.bundleName  = bundleName
        }

        HttpGet pingEndpoint() {
            return new HttpGet(createUrl(host, port, API_PATH, "/ping"))
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

        HttpGet getProjectEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, projectSuffix()))) as HttpGet
        }

        HttpPut putProjectEndpoint() {
            withTokenHeader(new HttpPut(createUrl(host, port, API_PATH, projectSuffix()))) as HttpPut
        }

        HttpGet listBundlesEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, bundlesSuffix()))) as HttpGet
        }

        HttpGet getBundleEndpoint() {
            withTokenHeader(new HttpGet(createUrl(host, port, API_PATH, bundleSuffix()))) as HttpGet
        }

        HttpPost postDoopBundleEndpoint() {
            withTokenHeader(new HttpPost(createUrl(host, port, API_PATH, bundlesSuffix() + "?family=doop"))) as HttpPost
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

        String bundlesSuffix() {
            return "${projectSuffix()}/bundles"
        }

        String bundleSuffix() {
            if (!bundleName) throw new RuntimeException("No bundle name")
            return "${bundlesSuffix()}/$bundleName"
        }

        static final String createUrl(String host, int port, String path, String endPoint) {
            return "http://${host}:${port}${path}${endPoint}" as String
        }
    }
}