package com.clyze.client.web

import groovy.transform.CompileStatic
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

@CompileStatic
class SnapshotInput {
    final boolean isFile
    final String key
    String value

    SnapshotInput(boolean isFile, String key, String value) {
        this.isFile = isFile
        this.key = key
        this.value = value
    }

    void addTo(MultipartEntityBuilder entityBuilder) {
        if (isFile)
            entityBuilder.addPart(key, new FileBody(new File(value)))
        else
            entityBuilder.addPart(key, new StringBody(value))
    }
}
