package com.clyze.client.web.http

import com.clyze.client.web.api.LowLevelAPI
import groovy.transform.CompileStatic
import org.apache.http.HttpEntity

@CompileStatic
abstract class HttpStringClientCommand extends HttpClientCommand<String> {
    HttpStringClientCommand(HttpClientLifeCycle httpClientLifeCycle) {
        super(httpClientLifeCycle)
    }

    @Override
    String onSuccess(HttpEntity entity) {
        return LowLevelAPI.Responses.parseJson(entity) as String
    }
}
