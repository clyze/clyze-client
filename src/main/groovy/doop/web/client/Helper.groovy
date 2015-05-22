package doop.web.client

import doop.core.AnalysisOption
import doop.core.Doop
import doop.input.*
import groovy.transform.TypeChecked
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.log4j.Logger

/**
 * Created by saiko on 20/5/2015.
 */
@TypeChecked
class Helper {

    static void addFilesToMultiPart(String name, List<String> files, MultipartEntityBuilder builder) {
        if (files) {
            Logger.getRootLogger().debug "Adding $files to post"
            InputResolutionContext ctx = new DefaultInputResolutionContext(new ChainResolver(
                new FileResolver(),
                new DirectoryResolver()
            ))
            ctx.add(files)
            ctx.resolve()
            List<File> localFiles = ctx.getAll()
            Logger.getRootLogger().debug "Resolved $files -> $localFiles"
            localFiles.each { File f -> builder.addPart(name, new FileBody(f)) }
        }
    }


    static void buildPostRequest(String id, String name, Map<String, AnalysisOption> options, List<String> jars,
                                 MultipartEntityBuilder builder) {

        if (!name) throw new RuntimeException("The name option is not specified")
        if (!jars) throw new RuntimeException("The jar option is not specified")

        //add the name
        builder.addPart("name", new StringBody(name))

        //add the jars
        jars.each{ String jar ->
            try {
                addFilesToMultiPart("jar", [jar], builder)
            }
            catch(e) {
                //jar is not a local file
                Logger.getRootLogger().debug(e.getMessage(), e)
                Logger.getRootLogger().warn("$jar is not a local file, it will be posted as string.")
                builder.addPart("jar", new StringBody(jar))
            }
        }

        //add the id
        if (id) builder.addPart("id", new StringBody(id))

        //add the options
        Logger.getRootLogger().debug "Adding options: $options"
        options.each { Map.Entry<String, AnalysisOption> entry ->
            String optionName = entry.getKey()
            AnalysisOption option = entry.getValue()
            if (option.value) {
                if (optionName == "DYNAMIC") {
                    List<String> dynamicFiles = option.value as List<String>
                    addFilesToMultiPart("DYNAMIC", dynamicFiles, builder)
                } else if (option.isFile) {
                    addFilesToMultiPart(optionName, [option.value as String], builder)
                } else {
                    Logger.getRootLogger().debug "Adding $optionName = ${option.value} to post"
                    builder.addPart(optionName, new StringBody(option.value as String))
                }
            }
        }
    }


    static List collectWithIndex(def collection, Closure closure) {
        int i = 1
        return collection.collect { closure.call(it, i++) }
    }
}
