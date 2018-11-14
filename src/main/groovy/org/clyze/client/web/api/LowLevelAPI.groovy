package org.clyze.client.web.api

import groovy.json.JsonSlurper

import org.clyze.client.web.http.HttpClientCommand

import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair

import groovy.transform.CompileStatic 

@CompileStatic
class LowLevelAPI {

    public static final String BASE_PATH    = "/clue"
	public static final String API_PATH     = "${BASE_PATH}/api/v1"
    public static final String HEADER_TOKEN = "x-clue-token"    

    static final class InputConstants {
        public static final String INPUTS   = "INPUTS"
        public static final String PLATFORM = "PLATFORM"
        public static final String ANALYSIS = "ANALYSIS"
    }

    static final String createUrl(String host, int port, String path, String endPoint) {
        return "http://${host}:${port}${path}${endPoint}" as String
    }

    static final def asJson(HttpEntity entity) {
        def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
        return json
    }

    static final class Requests {

        static final HttpGet ping(String host, int port) {
            return new HttpGet(createUrl(host, port, API_PATH, "/ping"))
        }

        static final HttpPost cleanDeploy(String host, int port) {
            return new HttpPost(createUrl(host, port, BASE_PATH, "/clean/deploy"))
        }

        static final HttpPost login(String username, String password, String host, int port) {
            HttpPost post = new HttpPost(createUrl(host, port, API_PATH, "/authenticate"))
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }        

        static final HttpGet listBundles(String userToken, String host, int port) {
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/bundles"))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }

        static final HttpPost createDoopBundle(String userToken, MultipartEntityBuilder entityBuilder, String host, int port) {            
            HttpPost post = new HttpPost(createUrl(host, port, API_PATH, "/bundles?family=doop"))
            if (userToken) post.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)            
            post.setEntity(entityBuilder.build())
            return post        
        }

        static final HttpPost createDoopBundle(String userToken, String platform, String bundleResolvableByServer, String host, int port) {                    
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(LowLevelAPI.InputConstants.INPUTS, new StringBody(bundleResolvableByServer))
            entityBuilder.addPart(LowLevelAPI.InputConstants.PLATFORM, new StringBody(platform))
            return createDoopBundle(userToken, entityBuilder, host, port)
        }

        static final HttpPost createAnalysis(String userToken, String bundleId, String analysis, String host, int port) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(LowLevelAPI.InputConstants.ANALYSIS, new StringBody(analysis))                
            return createAnalysis(userToken, bundleId, entityBuilder, host, port)
        }

        static final HttpPost createAnalysis(String userToken, String bundleId, MultipartEntityBuilder entityBuilder, String host, int port) {
            HttpPost post = new HttpPost(LowLevelAPI.createUrl(host, port, LowLevelAPI.API_PATH, "/bundles/${bundleId}/analyses"))
            if (userToken) post.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)            
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPut executeAnalysisAction(String userToken, String bundleId, String analysis, String action, String host, int port) {
            HttpPut put = new HttpPut(createUrl(host, port, API_PATH, "/bundles/${bundleId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(HEADER_TOKEN, userToken)
            return put
        }

        static final HttpGet getAnalysisStatus(String userToken, String bundleId, String analysis, String host, int port) {
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/bundles/${bundleId}/analyses/${analysis}"))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }

        static final HttpGet getSymbolAt(String userToken, String bundleId, String analysisId, String file, int line, int col, String host, int port) {
            String fileEncoded = URLEncoder.encode(file, "UTF-8")       
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/bundles/${bundleId}/symbols/${fileEncoded}/${line}/${col}?analysis=${analysisId}"))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }        

        static final HttpGet getOptionsForCreate(String what, String host, int port) {
            return new HttpGet(createUrl(host, port, API_PATH, "/options?what=${what}"))
        }        

        static final HttpGet getUsers(String userToken, String host, int port) {
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/users"))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }

        static final HttpPost createUser(String userToken, String username, String password, String host, int port) {
            HttpPost post = new HttpPost(createUrl(host, port, API_PATH, "/users"))
            if (userToken) post.addHeader(HEADER_TOKEN, userToken)
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", username))
            params.add(new BasicNameValuePair("password", password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpDelete deleteUser(String userToken, String username, String host, int port) {
            HttpDelete delete = new HttpDelete(createUrl(host, port, API_PATH, "/users/${username}"))
            if (userToken) delete.addHeader(HEADER_TOKEN, userToken)
            return delete
        }        

        static final HttpGet getProjects(String userToken, String host, int port) {
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/projects"))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }

        static final HttpPost createProject(String userToken, String projectName, String host, int port) {
            HttpPost post = new HttpPost(createUrl(host, port, API_PATH, "/projects"))
            if (userToken) post.addHeader(HEADER_TOKEN, userToken)
            List<NameValuePair> params = new ArrayList<>(1)
            params.add(new BasicNameValuePair("name", projectName))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        }

        static final HttpGet getProject(String userToken, String id, String host, int port) {
            HttpGet get = new HttpGet(createUrl(host, port, API_PATH, "/projects/" + id))
            if (userToken) get.addHeader(HEADER_TOKEN, userToken)
            return get
        }

        static final HttpPut updateProject(String userToken, String projectId, String newName, List<String> newMembers, String host, int port) {
            HttpPut put = new HttpPut(createUrl(host, port, API_PATH, "/projects/" + projectId))
            if (userToken) put.addHeader(HEADER_TOKEN, userToken)
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("name", newName))
            newMembers?.each {
                params.add(new BasicNameValuePair("members", it))            
            }
            put.setEntity(new UrlEncodedFormEntity(params))
            return put
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
}