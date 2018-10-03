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

    static final HttpClientCommand PING = new HttpClientCommand(                
        requestBuilder: { String host, int port ->
            return new HttpGet("http://${host}:${port}${API_PATH}/ping")
        }
    )

    static final HttpClientCommand CLEAN_DEPLOY = new HttpClientCommand(        
        requestBuilder: { String host, int port ->
            return new HttpPost("http://${host}:${port}${BASE_PATH}/clean/deploy")
        }
    )

    static final HttpClientCommand<String> LOGIN = new HttpClientCommand<>(        
        requestBuilder: { String host, int port ->
            return new HttpPost("http://${host}:${port}${API_PATH}/authenticate")
        },
        onSuccess: { HttpEntity entity ->
             def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
             return json.token as String
        }
    )

    static final HttpClientCommand<Map> LIST_BUNDLES = new HttpClientCommand<>(        
        requestBuilder: { String host, int port ->
            return new HttpGet("http://${host}:${port}${API_PATH}/bundles")
        },
        onSuccess: { HttpEntity entity ->
             def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
             return json
        }

    )
}