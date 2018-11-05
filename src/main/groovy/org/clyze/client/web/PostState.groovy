package org.clyze.client.web

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import org.clyze.persistent.model.Item

import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

import org.apache.commons.cli.Option

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class PostState implements Item {

    private static final Log logger = LogFactory.getLog(getClass())   

    String id
    List<Input> inputs = []

    static class Input {
        String key
        String value
        boolean isFile = false 
    }    

    PostState() { }

    @Override
    PostState fromJSON(String json) {
        return this
    }

    @Override
    String toJSON() { 
        //return new Gson().toJson();
        return null
    }

    @Override
    Map<String, Object> toMap() {
        return null
    }

    void saveTo(File dir) {
        //TODO
    }

    void addStringInput(String key, String value) {
        inputs.add(new Input(key:key, value:value))
    }

    void addFileInput(String key, String file) {
        inputs.add(new Input(key:key, value:file, isFile:true))
    }

    void addInputFromCliOption(Option o, OptionAccessor cliOptions) {
        String oid = o.longOpt.toUpperCase()
        String[] values = cliOptions.getOptionValues(o.longOpt)
        if (o.argName && o.argName.startsWith('file')) {
            values.each {
                addFileInput(oid, it)
            }
        }
        else {
            values.each {
                addStringInput(oid, it)
            }
        }
    }

    MultipartEntityBuilder asMultipart() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()              
        inputs.each { Input input ->
            if (input.isFile) {
                File f = new File(input.value)
                if (f.exists()) {
                    logger.debug("$input.value is a local file, it will be posted as attachment.")
                    builder.addPart(input.key, new FileBody(f))
                }
                else {
                    //not a local file
                    logger.debug("$input.value is not a local file, it will be posted as text.")
                    builder.addPart(input.key, new StringBody(input.value))
                }
            }
            else {
                builder.addPart(input.key, new StringBody(input.value))
            }       
        }
        builder
    }
    
    private static String fileName(String f) {
        int idx = f.lastIndexOf(File.separator);
        return (idx == -1) ? f : f.substring(idx+1)
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

    private static void checkFlatStructure(def options, String sourcesName, String jcPluginMetadataName, List heapDLNames) {
        List l = []
        l.addAll(options.inputs)
        l.add(sourcesName)
        l.add(jcPluginMetadataName)
        l.addAll(options.libraries)
        l.addAll(heapDLNames)
        l = l.findAll { it != null }
        if (l.size() != l.toSet().size())
            throw new RuntimeException("Flat structure violation, duplicate elements found in: ${l}")
    }
}
