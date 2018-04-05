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
    List<File> hprofs

    // Needed for JSON deserialization.
    public PostState() { }

    public PostState(String host, int port, String username, String password,
                     String orgName, String projectName, String projectVersion,
                     String rating, String ratingCount,
                     Map<String, Object> options, File sources,
                     File jcPluginMetadata, List<File> hprofs) {
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
        this.hprofs           = hprofs
    }

    public String toJson() {
        def options2 = options.clone()
        options2.inputs = options2.inputs.findAll { it != null }
                                         .collect { fileName(it) }
        options2.libraries = options2.libraries.findAll { it != null }
                                               .collect { fileName(it) }
        String optionsJson = new JsonBuilder(options2).toPrettyString()
        String sourcesName = sources == null? "null" : "\"${sources.name}\""
        String jcPluginMetadataName = jcPluginMetadata == null? "null" : "\"${jcPluginMetadata.name}\""
        List<String> hprofNames = hprofs.findAll { it != null }
                                        .collect { "\"${it.name}\"" }
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
            "  \"hprofNames\" : ${hprofNames}\n" +
            "}"
    }

    public static def fromJson(String dir, String analysisJsonName) {
        File jsonFile = new File("${dir}/${analysisJsonName}")
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

        // Fix the paths of inputs/libraries to point to the given directory.
        def dirFile = { String n -> n == null ? null : new File("${dir}/${n}") }
        ps.options.inputs = ps.options.inputs.findAll { it != null }
                                             .collect { dir + "/" + fileName(it) }
        ps.options.libraries = ps.options.libraries.findAll { it != null }
                                                   .collect { dir + "/" + fileName(it) }

        ps.sources = dirFile(obj.sourcesName)
        ps.jcPluginMetadata = dirFile(obj.jcPluginMetadataName)
        ps.hprofs = obj.hprofNames.collect { dirFile(it) }

        return (PostState)ps
    }

    private static String fileName(String f) {
        int idx = f.lastIndexOf(File.separator);
        return (idx == -1) ? f : f.substring(idx+1)
    }

    // Generate shell script to runs Doop with this state's options.
    public String generateDoopScript(String dir) {
        String script = '#!/bin/bash' + '\n' + '\n' +
            'if [ "${DOOP_HOME}" == "" ]; then' + '\n' +
            '    echo "Plase set DOOP_HOME."' + '\n' +
            '    exit' + '\n' +
            'fi' + '\n' + '\n' +
            'if [ "${PROJECT_DIR}" == "" ]; then' + '\n' +
            '    PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]-$0}" )" && pwd )"' + '\n' +
            '    echo "Using PROJECT_DIR=${PROJECT_DIR}"' + '\n' +
            'fi' + '\n' + '\n' +
            'pushd $DOOP_HOME' + '\n' +
            ''
        List<String> cmdLine = [ "./doop" ]
        Closure projFile = { String p -> '${PROJECT_DIR}/' + fileName(p) }
        options.each { String opt, Object val ->
            String option = opt?.replaceAll('_', '-')
            if (option == 'inputs') {
                cmdLine << "-i"
                val.each { cmdLine << projFile(it) }
            }
            else if (option == 'libraries') {
                if (val?.size() > 0) {
                    cmdLine << "-l"
                    val.each { cmdLine << projFile(it) }
                }
            }
            else if (option == 'analysis') {
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
        if ((hprofs != null) && (hprofs.size() > 0)) {
            cmdLine << "--heapdl"
            hprofs.each { hprof -> cmdLine << projFile(hprof.canonicalPath) }
        }
        cmdLine << '"$@"'
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
