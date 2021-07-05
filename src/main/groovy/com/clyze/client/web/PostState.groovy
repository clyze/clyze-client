package com.clyze.client.web

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.clyze.persistent.model.ItemImpl

import static org.apache.commons.io.FileUtils.copyFileToDirectory

@Log4j
@CompileStatic
class PostState extends ItemImpl {

    String id
    List<String> stacks
    private Map<String, SnapshotInput> inputs = new HashMap<>()

    PostState() { }

    /**
     * Generate JSON with file paths made relative to a
     * given prefix. Paths that do not start with the
     * prefix are ignored.
     * @param prefix   the path prefix to remove
     */
    String toJSONWithRelativePaths(final String prefix) {
        final int prefixLen = prefix.length() + File.pathSeparator.length()
        List<SnapshotInput> inputs0 = inputs.collect { Map.Entry<String, SnapshotInput> entry ->
            SnapshotInput i0 = entry.value
            if (i0.isFile) {
                String path = i0.value
                if (path.startsWith(prefix))
                    return new SnapshotInput(i0.isFile, path.substring(prefixLen))
            }
            return i0
        }
        return JsonOutput.toJson([inputs: inputs0, stacks: stacks] as Map<String, Object>)
    }

    @Override
    protected void saveTo(Map<String, Object> map) {
        map.put('inputs', inputs)
        map.put('stacks', stacks)
    }

    @Override
    void fromMap(Map<String, Object> map) {
        (map.inputs as Map<String, SnapshotInput>).each { Map.Entry<String, SnapshotInput> entry ->
            SnapshotInput it = entry.value
            if (it.isFile)
                addFileInput(entry.key, it.value)
            else
                addStringInput(entry.key, it.value)
        }
        this.stacks = (List<String>) map.get('stacks')
    }

    PostState saveTo(File dir) {
        //process inputs to copy all files in the given dir
        inputs.findAll { it.value.isFile }.values().each { SnapshotInput input ->
            log.info "Copying: ${input.value} -> ${dir}"
            File f = new File(input.value)
            if (f.exists()) {
                String name = f.getName()
                File dest = new File(dir, name)
                if (dest.exists())
                    log.warn "WARNING: overwriting $name in $dir"
                copyFileToDirectory(f, dir)
                input.value = new File(dir, name).canonicalPath
            }
        }   
        File json = new File(dir, id + ".json")
        // json.text = toJSON()
        json.text = toJSONWithRelativePaths(dir.canonicalPath)

        return this
    }

    PostState loadFrom(File dir) {
        File json = new File(dir, id + ".json") 
        if (json.exists()) {
            return fromJSON(json.text) as PostState
        } else {
            throw new RuntimeException("File ${id}.json not found in $dir")
        }        
    }

    /**
     * Loads a PostState containing relative paths from a directory, making
     * the paths absolute by prepending the directory path.
     *
     * @param dir  the directory containing the data
     * @return     a PostState object with absolute paths
     */
    PostState loadAndTranslatePathsFrom(File dir) {
        PostState ps = loadFrom(dir)
        ps.inputs.findAll { it.value.isFile }.values().each {
            // Ignore full paths on Unix.
            if (!it.value.startsWith(File.separator))
                it.value = (new File(dir, it.value)).canonicalPath
        }
        return ps
    }

    void addStringInput(String key, String value) {
        inputs.put(key, new SnapshotInput(false, value))
    }

    void addFileInput(String key, String file) {
        inputs.put(key, new SnapshotInput(true, file))
    }

    MultipartEntityBuilder asMultipart() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
        inputs.each { it.value.addTo(it.key, builder) }
        return builder
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

    Map<String, SnapshotInput> getInputs() {
        return this.inputs
    }

    void addInput(String key, SnapshotInput input) {
        SnapshotInput existing = inputs.get(key)
        if (existing != null)
            log.warn "WARNING: overriding key '${key}': ${existing} -> ${input}"
        inputs.put(key, input)
    }
}
