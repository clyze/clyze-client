package com.clyze.client.web

import groovy.transform.CompileStatic
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

@CompileStatic
class SnapshotInput {
    final boolean isFile
    String value

    SnapshotInput(boolean isFile, String value) {
        this.isFile = isFile
        this.value = value
    }

    void addTo(String key, MultipartEntityBuilder entityBuilder) {
        if (isFile)
            entityBuilder.addPart(key, new FileBody(new File(value)))
        else
            entityBuilder.addPart(key, new StringBody(value))
    }

    @Override
    String toString() {
        return isFile ? "file<${value}>" : "string<${value}>"
    }
}