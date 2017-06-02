package org.clyze.client.web

//import groovy.transform.TypeChecked
import groovy.json.JsonSlurper

import org.clyze.analysis.AnalysisOption
import org.clyze.analysis.AnalysisFamilies
import org.clyze.doop.core.*
import org.clyze.doop.input.*
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.message.BasicNameValuePair


//@TypeChecked
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
     * Returns true if the analysis option indicated by the given id is a file option.
     */
    static boolean isFileOption(String id) {    
        //Quick fix - the life-cycle of analysis families needs discussion
        if (!AnalysisFamilies.isRegistered('doop')) {
            AnalysisFamilies.register(DoopAnalysisFamily.instance)
        }
        AnalysisOption option  = AnalysisFamilies.supportedOptionsOf('doop').find {AnalysisOption option -> option.id == id}
        return option ? option.isFile : false
    }


    /**
     * Creates the default login command (that returns the token when executed).
     */
    static RestCommandBase<String> createLoginCommand(String username, String password) {
        return new RestCommandBase<String>(
            endPoint: "authenticate",
            authenticationRequired: false,
            requestBuilder: { String url ->
                HttpPost post = new HttpPost(url)
                List<NameValuePair> params = new ArrayList<>(2)
                params.add(new BasicNameValuePair("username", username))
                params.add(new BasicNameValuePair("password", password))
                post.setEntity(new UrlEncodedFormEntity(params))
                return post
            },
            onSuccess: { HttpEntity entity ->
                def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
                return json.token
            }
        )
    }


    /**
     * Creates a post doop analysis command (without authenticator) that returns the id of the newly created doop analysis.
     */
    static RestCommandBase<String> createPostDoopAnalysisCommand(String projectName,
                                                                 String projectVersion,
                                                                 File sources, 
                                                                 File jcPluginMetadata,
                                                                 File hprof,
                                                                 Map<String, Object> options) {
        return new RestCommandBase<String>(
            endPoint: "family/doop",
            requestBuilder: { String url ->
                HttpPost post = new HttpPost(url)
                MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                //submit a null id for the analysis to make the server generate one automatically
                buildPostRequest(builder, null, options.analysis) {

                    //process the project name and version
                    builder.addPart("projectName", new StringBody(projectName))
                    builder.addPart("projectVersion", new StringBody(projectVersion))

                    //process the sources
                    println "Submitting sources: ${sources}"
                    Helper.addFilesToMultiPart("sources", [sources], builder)

                    //process the jcPluginMetadata
                    println "Submitting jcplugin metadata: ${jcPluginMetadata}"
                    Helper.addFilesToMultiPart("jcPluginMetadata", [jcPluginMetadata], builder)

                    //process the HPROF file
                    if (hprof != null) {
                        println "Submitting HPROF: ${hprof}"
                        Helper.addFilesToMultiPart("ANALYZE_MEMORY_DUMP", [hprof], builder)
                    }

                    //process the options                    
                    println "Submitting options: ${options}"
                    options.each { Map.Entry<String, Object> entry ->
                        String optionId = entry.key.toUpperCase()
                        def value = entry.value
                        if (value) {
                            if (optionId == "INPUTS" || optionId == "DYNAMIC") {
                                Helper.addFilesToMultiPart(optionId, value, builder)
                            }
                            else if (Helper.isFileOption(optionId)) {
                                Helper.addFilesToMultiPart(optionId, [new File(value)], builder)
                            }
                            else {
                                builder.addPart(optionId, new StringBody(value as String))
                            }
                        }
                    }
                }
                HttpEntity entity = builder.build()
                post.setEntity(entity)
                return post
            },
            onSuccess: { HttpEntity entity ->
                def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
                return json.id
            }            
        )
    }


    /**
     * Creates a start analysis command (without authenticator and onSuccess handlers).
     */
    static RestCommandBase<Void> createStartCommand(String id) {
        return new RestCommandBase<Void>(
            endPoint: "analyses",
            requestBuilder: {String url ->
                return new HttpPut("${url}/${id}/action/start")
            }
        )
    }
}
