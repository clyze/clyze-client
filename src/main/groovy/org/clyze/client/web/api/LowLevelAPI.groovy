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
        return "http://${host}:${port}${path}${endPoint}"
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
            return new HttpPost(createUrl(host, port, BASE_PATH, "/clen/deploy"))
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

        static final HttpPost createDoopBundle(String userToken, String platform, String bundleResolvableByServer, String host, int port) {        
            HttpPost post = new HttpPost(createUrl(host, port, API_PATH, "/bundles?family=doop"))
            if (userToken) post.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(LowLevelAPI.InputConstants.INPUTS, new StringBody(bundleResolvableByServer))
            entityBuilder.addPart(LowLevelAPI.InputConstants.PLATFORM, new StringBody(platform))
            post.setEntity(entityBuilder.build())
            return post        
        }

        static final HttpPost createAnalysis(String userToken, String bundleId, String analysis, String host, int port) {
            HttpPost post = new HttpPost(LowLevelAPI.createUrl(host, port, LowLevelAPI.API_PATH, "/bundles/${bundleId}/analyses"))
            if (userToken) post.addHeader(LowLevelAPI.HEADER_TOKEN, userToken)
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()              
            entityBuilder.addPart(LowLevelAPI.InputConstants.ANALYSIS, new StringBody(analysis))                
            post.setEntity(entityBuilder.build())
            return post
        }

        static final HttpPut executeAnalysisAction(String userToken, String bundleId, String analysis, String action, String host, int port) {
            HttpPut put = new HttpPut(createUrl(host, port, API_PATH, "/bundles/${bundleId}/analyses/${analysis}/action/${action}"))
            if (userToken) put.addHeader(HEADER_TOKEN, userToken)
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


    static final HttpClientCommand<String> CREATE_ANALYSIS = new HttpClientCommand<>(        
        onSuccess: { HttpEntity entity ->
             def json = asJson(entity)
             return json.id as String
        }
    )
}