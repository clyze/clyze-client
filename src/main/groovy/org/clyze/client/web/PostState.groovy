package org.clyze.client.web

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

// Serialized information needed to replay analysis posting.
class PostState {
    String host
    int port
    String username
    String password
    String orgName
    String projectName
    String projectVersion
    String rating
    String ratingCount
    Map<String, Object> options
    File sources
    File jcPluginMetadata
    File hprof

    // Needed for JSON deserialization.
    public PostState() { }

    public PostState(String host, int port, String username, String password,
                     String orgName, String projectName, String projectVersion,
                     String rating, String ratingCount,
                     Map<String, Object> options, File sources,
                     File jcPluginMetadata, File hprof) {
        this.username         = username
        this.password         = password
        this.host             = host
        this.port             = port
        this.orgName          = orgName
        this.projectName      = projectName
        this.projectVersion   = projectVersion
        this.rating           = rating
        this.ratingCount      = ratingCount
        this.options          = options
        this.sources          = sources
        this.jcPluginMetadata = jcPluginMetadata
        this.hprof            = hprof
    }

    public String toJson() {
        String optionsJson = new JsonBuilder(options).toPrettyString()
        String sourcesName = sources == null? "null" : "\"${sources.name}\""
        String jcPluginMetadataName = jcPluginMetadata == null? "null" : "\"${jcPluginMetadata.name}\""
        String hprofName = hprof == null? "null" : "\"${hprof.name}\""
        return "{ \"host\" : \"${host}\",\n" +
            "  \"port\" : \"${port}\",\n" +
            "  \"username\" : \"${username}\",\n" +
            "  \"password\" : \"${password}\",\n" +
            "  \"orgName\" : \"${orgName}\",\n" +
            "  \"projectName\" : \"${projectName}\",\n" +
            "  \"projectVersion\" : \"${projectVersion}\",\n" +
            "  \"rating\" : \"${rating}\",\n" +
            "  \"ratingCount\" : \"${ratingCount}\",\n" +
            "  \"options\" : ${optionsJson},\n" +
            "  \"sourcesName\" : ${sourcesName},\n" +
            "  \"jcPluginMetadataName\" : ${jcPluginMetadataName},\n" +
            "  \"hprofName\" : ${hprofName}\n" +
            "}"
    }

    public static def fromJson(String dir) {
        File jsonFile = new File("${dir}/analysis.json")
        Object obj = (new JsonSlurper()).parseText(jsonFile.text)
        PostState ps = new PostState()
        ps.host           = obj.host
        ps.port           = obj.port.toInteger()
        ps.username       = obj.username
        ps.password       = obj.password
        ps.orgName        = obj.orgName
        ps.projectName    = obj.projectName
        ps.projectVersion = obj.projectVersion
        ps.rating         = obj.rating
        ps.ratingCount    = obj.ratingCount
        ps.options        = obj.options

        // Fix the paths of inputs to point to the given directory.
        def dirFile = { String n -> n == null ? null : new File("${dir}/${n}") }
        def fileName = { String f -> f.substring(f.lastIndexOf(File.separator)) }
        ps.options.inputs = ps.options.inputs.findAll { it != null }
                                             .collect { dir + "/" + fileName(it) }

        ps.sources = dirFile(obj.sourcesName)
        ps.jcPluginMetadata = dirFile(obj.jcPluginMetadataName)
        ps.hprof = dirFile(obj.hprofName)

        return (PostState)ps
    }
}
