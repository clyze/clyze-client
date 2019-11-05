package org.clyze.client.web.api

import org.apache.http.HttpEntity

interface AttachmentHandler<T> {
    T handleAttachment(HttpEntity entity)
}
