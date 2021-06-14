package com.clyze.client.web.http

import com.clyze.client.web.api.LowLevelAPI
import groovy.transform.CompileStatic
import org.apache.http.HttpEntity

@CompileStatic
abstract class HttpMapClientCommand extends HttpClientCommand<Map<String, Object>> {

    HttpMapClientCommand(HttpClientLifeCycle httpClientLifeCycle) {
        super(httpClientLifeCycle)
    }

    @Override
    Map<String, Object> onSuccess(HttpEntity entity) {
        return LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
    }
}
