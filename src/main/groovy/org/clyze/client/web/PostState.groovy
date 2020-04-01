package org.clyze.client.web

import groovy.cli.commons.OptionAccessor
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.TypeChecked
import groovy.util.logging.Log4j
import org.apache.commons.cli.Option
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.clyze.persistent.model.Item

import static org.apache.commons.io.FileUtils.copyFileToDirectory

@Log4j
@TypeChecked
class PostState implements Item {

    String id
    Set<Input> inputs = new HashSet<>()

    static class Input {
        String key
        String value
        boolean isFile = false 
    }    

    PostState() { }

    @Override
    PostState fromJSON(String json) {
        def obj = new JsonSlurper().parseText(json)        
        fromMap(obj as Map<String, Object>)
        this
    }

    @Override
    String toJSON() { 
        return JsonOutput.toJson(toMap())
    }

    /**
     * Generate JSON with file paths made relative to a
     * given prefix. Paths that do not start with the
     * prefix are ignored.
     * @param prefix   the path prefix to remove
     */
    String toJSONWithRelativePaths(final String prefix) {
        final int prefixLen = prefix.length() + File.pathSeparator.length()
        List<Input> inputs0 = inputs.collect {
            if (it instanceof Input) {
                Input i0 = (Input)it
                if (i0.isFile) {
                    String path = i0.value
                    if (path.startsWith(prefix))
                        return new Input(key:i0.key, value:path.substring(prefixLen), isFile:i0.isFile)
                }
            }
            return it
        }
        return JsonOutput.toJson([inputs: inputs0] as Map<String, Object>)
    }

    @Override
    Map<String, Object> toMap() {
        return [            
            inputs: inputs
        ] as Map<String, Object>
    }

    @Override
    void fromMap(Map<String, Object> map) {
        (map.inputs as Set<Input>).each {
            if (it.isFile) {
                addFileInput(it.key as String, it.value)
            } else {
                addStringInput(it.key as String, it.value)
            }
        }
    }

    PostState saveTo(File dir) {
        //process inputs to copy all files in the given dir
        inputs.findAll { it.isFile }.each { Input input ->
            log.info "Copying: ${input.value} -> ${dir}"
            File f = new File(input.value)
            if (f.exists()) {
                String name = f.getName()
                File dest = new File(dir, name)
                if (dest.exists()) {
                    throw new RuntimeException("File $name already exists in $dir")
                }
                copyFileToDirectory(f, dir)
                input.value = new File(dir, name).canonicalPath
            }
        }   
        File json = new File(dir, id + ".json")
        json.text = toJSON()

        return this
    }

    PostState loadFrom(File dir) {
        File json = new File(dir, id + ".json") 
        if (json.exists()) {
            return fromJSON(json.text)
        } else {
            throw new RuntimeException("File ${id}.json not found in $dir")
        }        
    }

    void addStringInput(String key, String value) {
        inputs.add(new Input(key:key, value:value))
    }

    void addFileInput(String key, String file) {
        inputs.add(new Input(key:key, value:file, isFile:true))
    }

    void addInputFromCliOption(Option o, OptionAccessor cliOptions) {
        String oid = o.longOpt.toUpperCase()
        String[] values = cliOptions.commandLine.getOptionValues(o.longOpt)
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
                    log.debug("${input.value} is a local file, it will be posted as attachment.")
                    builder.addPart(input.key, new FileBody(f))
                } else {
                    //not a local file
                    log.debug("${input.value} is not a local file, it will be posted as text.")
                    builder.addPart(input.key, new StringBody(input.value))
                }
            } else {
                builder.addPart(input.key, new StringBody(input.value))
            }       
        }
        builder
    }
    
    private static String fileName(String f) {
        int idx = f.lastIndexOf(File.separator)
        return (idx == -1) ? f : f.substring(idx+1)
    }

//    // Used to check if an object can be simply converted to a string.
//    private static boolean isPrimitiveOrString(Object obj) {
//        return (obj.class in [ String.class, Boolean.class, Character.class,
//                               Byte.class, Short.class, Integer.class, Long.class,
//                               Float.class, Double.class ])
//    }

    // Given a file path and a directory prefix of it, strip the
    // prefix from the path (or do nothing if the prefix is wrong).
    private static String stripDir(String fPath, String dir) {
        int prefixSz = dir.length()
        String fPrefix = fPath.substring(0, prefixSz)
        if (fPrefix == dir) {
            return fPath.substring(prefixSz + 1)
        } else {
            println "WARNING: ${fPath} is not under ${dir}"
            return fPath
        }
    }

//    private static void checkFlatStructure(def options, String sourcesName, String jcPluginMetadataName, List heapDLNames) {
//        List l = []
//        l.addAll(options.inputs)
//        l.add(sourcesName)
//        l.add(jcPluginMetadataName)
//        l.addAll(options.libraries)
//        l.addAll(heapDLNames)
//        l = l.findAll { it != null }
//        if (l.size() != l.toSet().size())
//            throw new RuntimeException("Flat structure violation, duplicate elements found in: ${l}")
//    }
}
