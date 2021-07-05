package com.clyze.client.web

import groovy.transform.CompileStatic
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

@CompileStatic
class SnapshotInput {
    String key
    final boolean isFile
    String value

    SnapshotInput(String key, boolean isFile, String value) {
        this.key = key
        this.isFile = isFile
        this.value = value
    }

    void addTo(MultipartEntityBuilder entityBuilder) {
        if (isFile)
            entityBuilder.addPart(key, new FileBody(new File(value)))
        else
            entityBuilder.addPart(key, new StringBody(value))
    }

    @Override
    String toString() {
        return "${key}=" + (isFile ? "file<${value}>" : "string<${value}>")
    }
}
