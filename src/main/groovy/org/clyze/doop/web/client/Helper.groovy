package org.clyze.doop.web.client

import org.clyze.analysis.AnalysisOption
import org.clyze.doop.core.*
import org.clyze.doop.input.*
import groovy.transform.TypeChecked
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

/**
 * Created by saiko on 20/5/2015.
 */
@TypeChecked
class Helper {

    static List<File> resolveFiles(List<String> files) {
        if (files) {
            InputResolutionContext ctx = new DefaultInputResolutionContext(new ChainResolver(
                new FileResolver(),
                new DirectoryResolver()
            ))
            ctx.add(files)
            ctx.resolve()
            return ctx.getAll()
        }
        else {
            return []
        }
    }

    static void addFilesToMultiPart(String name, Collection<File> files, MultipartEntityBuilder builder) {
        files?.each { File f -> builder.addPart(name, new FileBody(f)) }
    }

    static void buildPostRequest(MultipartEntityBuilder builder,
                                 String id,
                                 String name,
                                 Closure jarAndOptionProcessor) {

        if (!name) throw new RuntimeException("The name option is not specified")

        //add the name
        builder.addPart("name", new StringBody(name))

        //add the id
        if (id) builder.addPart("id", new StringBody(id))

        jarAndOptionProcessor.call()
    }


    static List collectWithIndex(def collection, Closure closure) {
        int i = 1
        return collection.collect { closure.call(it, i++) }
    }

    /**
     * Creates a map of the default web analysis options.
     * This method could have been placed in the doop-gradle-plugin project, but it is placed here for two reasons:
     * (a) to allow it to be reused by all doop build plugins
     * (b) to avoid exposing doop as a direct dependency for the doop build plugins
     * @return a Map containing the options' ids and values.
     */
    static Map<String, Object> createDefaultOptions(){
        Map<String, Object> opts = new HashMap<>()
        Doop.createDefaultAnalysisOptions().values().findAll { AnalysisOption option ->
            option.webUI
        }.each { AnalysisOption option ->
            opts.put option.id.toLowerCase(), option.value
        }
        return opts
    }

    /**
     * Indicates whether the analysis option indicated by the given id is a file option.
     */
    static boolean isFileOption(String id) {    
        AnalysisOption option  = Doop.createDefaultAnalysisOptions().values().find {AnalysisOption option -> option.id == id}
        return option ? option.isFile : false
    }
}
