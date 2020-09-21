package com.clyze.client.web.api

import groovy.transform.CompileStatic
import org.apache.http.HttpEntity

@CompileStatic
interface AttachmentHandler<T> {
    T handleAttachment(HttpEntity entity)
}
