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
        def fileName = { String f ->
            int idx = f.lastIndexOf(File.separator);
            (idx == -1) ? f : f.substring(idx)
        }
        ps.options.inputs = ps.options.inputs.findAll { it != null }
                                             .collect { dir + "/" + fileName(it) }

        ps.sources = dirFile(obj.sourcesName)
        ps.jcPluginMetadata = dirFile(obj.jcPluginMetadataName)
        ps.hprof = dirFile(obj.hprofName)

        return (PostState)ps
    }

    // Generate shell script to runs Doop with this state's options.
    public String generateDoopScript(String dir) {
        String script = '#!/bin/bash' + '\n' + '\n' + 'pushd $DOOP_HOME' + '\n'
        List<String> cmdLine = [ "./doop" ]
        options.each { String opt, Object val ->
            String option = opt?.replaceAll('_', '-')
            if (option == 'inputs') {
                cmdLine << "-i"
                val.each { cmdLine << it }
            } else if (option == 'analysis') {
                cmdLine << "-a ${val}"
            } else if (option.startsWith("x-")) {
                String xOption = option.substring(2)
                if (val.class == Boolean.class) {
                    if (val == true) {
                        cmdLine << "-X${xOption}"
                    }
                } else if (val != null && isPrimitiveOrString(val)) {
                    cmdLine << "-X${xOption} ${val}"
                } else {
                    println "WARNING: unknown X option ${opt} = ${val}"
                }
            } else if (val == null) {
                println "WARNING: ignoring null entry ${option} in Doop script"
            } else if (val.class == Boolean.class) {
                if (val == true) {
                    cmdLine << "--${option}"
                }
            } else if (isPrimitiveOrString(val)) {
                cmdLine << "--${option} ${val}"
            } else {
                println "WARNING: unknown entry ${option} (${opt}) = ${val}"
            }
        }
        if (hprof != null) {
            cmdLine << "--heapdl"
            cmdLine << hprof.canonicalPath
        }
        script += cmdLine.join(" ")
        script += '\n' + 'popd' + '\n'
        return script
    }

    // Used to check if an object can be simply converted to a string.
    private static boolean isPrimitiveOrString(Object obj) {
        return (obj.class in [ String.class, Boolean.class, Character.class,
                               Byte.class, Short.class, Integer.class, Long.class,
                               Float.class, Double.class ])
    }

    // Given a file path and a directory prefix of it, strip the
    // prefix from the path (or do nothing if the prefix is wrong).
    private static String stripDir(String fPath, String dir) {
        int prefixSz = dir.length()
        String fPrefix = fPath.substring(0, prefixSz)
        if (fPrefix.equals(dir)) {
            return fPath.substring(prefixSz + 1)
        } else {
            println "WARNING: ${fPath} is not under ${dir}"
            return fPath
        }
    }
}
