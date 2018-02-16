package org.clyze.client.web

import org.clyze.doop.CommandLineAnalysisFactory
import org.clyze.analysis.AnalysisOption
import org.clyze.analysis.AnalysisFamilies
import org.clyze.doop.core.Doop
import org.clyze.utils.FileOps
import groovy.json.JsonSlurper
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import org.apache.log4j.Logger

/**
 * A command line client for a remote doop server.
 *
 * The client can execute the following commands via the remote server:
 * <ul>
 *     <li>login             - authenticate user.
 *     <li>ping              - check connection with server.
 *     <li>list_bundles      - list the available bundles.
 *     <li>post_doop_bundle  - create a new doop bundle.    
 *     <li>list              - list the available analyses.
 *     <li>post_doop         - create a new doop analysis.
 *     <li>post_cclyzer      - create a new cclyzer analysis.
 *     <li>get               - retrieves an analysis.
 *     <li>start             - start an analysis.
 *     <li>stop              - stop an analysis.
 *     <li>query             - query a complete analysis.
 *     <li>delete            - delete an analysis.
 * </ul>
 *
 * Experimentally, the client also supports fetching all the jars from Maven Central that match a free-text query.
 */
class CliRestClient {

    private static final int DEFAULT_LIST_SIZE = 20

    private static final Option ID = OptionBuilder.hasArg().withArgName('id').
                                                   withDescription('the analysis id').create('id')

    private static final Closure<String> DEFAULT_SUCCES = { HttpEntity entity ->
        return "OK"
    }

    private static final Closure<HttpUriRequest> DEFAULT_REQUEST_BUILDER = { String url ->
        return new HttpGet(url)
    }

    private static final Closure<Void> DEFAULT_AUTHENTICATOR = { String host, int port, HttpUriRequest request ->
        String token = Authenticator.getUserToken()
        if (!token) {
            //Ask for username and password
            COMMANDS.login.execute(host, port)
            token = Authenticator.getUserToken()
        }

        //send the token with the request
        request.addHeader(RestCommandBase.HEADER_TOKEN, token)
    }

    //Used by list analyses and list bundles commands
    private static final Closure<HttpUriRequest> LIST_REQUEST_BUILDER = { String url ->
        Integer start = 0
        Integer count = DEFAULT_LIST_SIZE

        if (cliOptions.s) {
            try {
                start = cliOptions.s as Integer
            }
            catch (all) {
                Logger.getRootLogger().warn("Option start (${cliOptions.s}) is not valid, using default ($start).")
            }                
        }

        if (cliOptions.l) {
            try {
                count = cliOptions.l as Integer
            }
            catch (all) {
                Logger.getRootLogger().warn("Option count (${cliOptions.l}) is not valid, using default ($count).")
            }
        }

        return new HttpGet("${url}?_start=${start}&_count=${count}")
    }

    private static final String processAnalysisData(Integer index, def analysisData) {

        String head = """${index == null ? "" : "$index."} ${analysisData.projectName}"""
        String sep  = ""
        head.length().times { sep += "#" }

        return """\
               ${sep}               
               ${head}               
               ${sep}                              
               Org      : ${analysisData.orgName}
               Project  : ${analysisData.projectName}
               Version  : ${analysisData.projectVersion}
               ID       : ${analysisData.id}
               Family   : ${analysisData.family}
               Name     : ${analysisData.name}               
               Inputs   : ${analysisData.inputs.join(", ")}
               Libraries: ${analysisData.libraries.join(", ")}
               Status   : ${analysisData.state}""".stripIndent()
    }

    /**
     * Logins the user via posting to /authenticate endpoint.
     */
    private static final CliRestCommand LOGIN = new CliRestCommand(
        name: 'login',
        description: 'login to the remote server',
        endPoint: 'authenticate',
        authenticationRequired: false,
        requestBuilder: { String url ->
            Map<String, String> credentials = Authenticator.askForCredentials()
            HttpPost post = new HttpPost(url)
            List<NameValuePair> params = new ArrayList<>(2)
            params.add(new BasicNameValuePair("username", credentials.username))
            params.add(new BasicNameValuePair("password", credentials.password))
            post.setEntity(new UrlEncodedFormEntity(params))
            return post
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            Authenticator.setUserToken(json.token)
            return "Logged in, token updated."
        }
    )


    /**
     * Consumes the GET /ping response, ignoring the result.     
     */
    private static final CliRestCommand PING = new CliRestCommand(
        name: 'ping',
        description: "Pings the remote server",
        endPoint: "ping",
        authenticationRequired: false,
        requestBuilder: DEFAULT_REQUEST_BUILDER,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the GET /bundles response, printing the result.
     */
    private static final CliRestCommand LIST_BUNDLES = new CliRestCommand(
        name: 'list_bundles',
        description: 'Lists the available bundles',
        endPoint: 'bundles',
        options: [
            OptionBuilder.withLongOpt('start').hasArg().withArgName('start-position').
                          withDescription('The start position of the list (default 0)').                          
                          create('s'),
            OptionBuilder.withLongOpt('limit').hasArg().withArgName('number-of-results').
                          withDescription("The size of the list (default ${DEFAULT_LIST_SIZE})").
                          create('l')  
        ],
        requestBuilder: LIST_REQUEST_BUILDER,
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")                
            int start = json.start as Integer
            return "Start: ${json.start}, count: ${json.list?.size()?:0} of ${json.hits} total results.\n" + json.list?.collect {
                it //TODO: print bundle attributes
            }.join("\n")
        }
    )

    /**
     * Posts to the /bundles endpoint, printing the result
     */
    private static final CliRestCommand POST_DOOP_BUNDLE = new CliRestCommand(
        name: 'post_doop_bundle',
        description: 'Posts a new doop input bundle',
        endPoint: 'bundles',
        options: [
            OptionBuilder.withLongOpt('inputs').hasArg().withArgName('input jar').
                          withDescription('The input jar(s)').                          
                          create('i'),
            OptionBuilder.withLongOpt('libraries').hasArg().withArgName('library jar').
                          withDescription('The library jar(s)').                          
                          create('l')
            //TODO: Add more options  
        ],
        requestBuilder: { String url ->
            HttpPost post = new HttpPost("${url}?family=doop")
            def inputs    = cliOptions.is
            def libraries = cliOptions.ls
            
            if (!inputs) throw new RuntimeException("No input jars")

            MultipartEntityBuilder builder = MultipartEntityBuilder.create() 
            inputs.each { String jar ->
                File f = new File(jar)
                if (f.exists()) {
                    Helper.addFilesToMultiPart("INPUTS", [f], builder)
                }
                else {
                    //jar is not a local file
                    println("$jar is not a local file, it will be posted as string.")
                    builder.addPart("INPUTS", new StringBody(jar))
                }                
            }           
            HttpEntity entity = builder.build()
            post.setEntity(entity)
            return post
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")    
            println json
        }
    )

    /**
     * Consumes the GET /analyses response, printing the result.     
     */
    private static final CliRestCommand LIST = new CliRestCommand(
        name: 'list',
        description: "Lists the analyses of the remote server",
        endPoint: "analyses",
        options: [
            OptionBuilder.withLongOpt('start').hasArg().withArgName('start-position').
                          withDescription('The start position of the list (default 0)').                          
                          create('s'),
            OptionBuilder.withLongOpt('limit').hasArg().withArgName('number-of-results').
                          withDescription("The size of the list (default ${DEFAULT_LIST_SIZE})").
                          create('l')  

        ],
        requestBuilder: LIST_REQUEST_BUILDER,
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")                
            int start = json.start as Integer
            return "Start: ${json.start}, count: ${json.list?.size()?:0} of ${json.hits} total results.\n" + json.list?.collect {
                processAnalysisData(start++, it)
            }.join("\n")
        }
    )

    /**
     * Posts to the analyses/family/doop endpoint, printing the result.     
     */
    private static final CliRestCommand POST_DOOP = new CliRestCommand(
        name: 'post_doop',
        description: "Posts a new doop analysis to the remote server",
        endPoint: "analyses/family/doop",
        options: [
                OptionBuilder.withLongOpt('properties').hasArg().withArgName('properties').
                        withDescription(CommandLineAnalysisFactory.PROPS).create('p'),
        ] + CommandLineAnalysisFactory.convertAnalysisOptionsToCliOptions(AnalysisFamilies.supportedOptionsOf('doop').findAll { it.webUI }),
        requestBuilder: { String url ->

            String name, id
            List<String> inputs, libraries
            Map<String, AnalysisOption> options

            if (cliOptions.p) {
                //load the analysis options from the property file
                String file = cliOptions.p
                File f = FileOps.findFileOrThrow(file, "Not a valid file: $file")
                File propsBaseDir = f.getParentFile()
                Properties props = Helper.loadProperties(file)

                //Get the name of the analysis
                name = cliOptions.a ?: props.getProperty("analysis")

                //Get the inputs of the analysis. If there are no inputs in the CLI, we get them from the properties.
                inputs = cliOptions.is
                if (!inputs) {
                    inputs = props.getProperty("INPUTS").split().collect { String s -> s.trim() }
                    //The inputs, if relative, are being resolved via the propsBaseDir
                    inputs = inputs.collect { String jar ->
                        File jarFile = new File(jar)
                        return jarFile.isAbsolute() ? jar : new File(propsBaseDir, jar).getCanonicalFile().getAbsolutePath()
                    }
                }

                //Get the libraries of the analysis. If there are no librraries in the CLI, we get them from the properties.
                libraries = cliOptions.ls
                if (!libraries) {
                    libraries = props.getProperty("LIBRARIES").split().collect { String s -> s.trim() }
                    //The libraries, if relative, are being resolved via the propsBaseDir
                    libraries = libraries.collect { String jar ->
                        File jarFile = new File(jar)
                        return jarFile.isAbsolute() ? jar : new File(propsBaseDir, jar).getCanonicalFile().getAbsolutePath()
                    }
                }

                //Get the optional id of the analysis
                id = cliOptions.id ?: props.getProperty("id")

                options = Doop.overrideDefaultOptionsWithPropertiesAndCLI(props, cliOptions) {AnalysisOption option -> option.webUI }
            }
            else {

                //Get the name of the analysis
                name = cliOptions.a ?: null
                //Get the inputs of the analysis
                inputs = cliOptions.is ?: null
                //Get the libraries of the analysis
                libraries = cliOptions.ls ?: null
                //Get the optional id of the analysis
                id = cliOptions.id ?: null

                options = Doop.overrideDefaultOptionsWithCLI(cliOptions) { AnalysisOption option -> option.webUI }
            }

            options = options.findAll { it.value.webUI }
            options["INPUTS"].value = inputs
            options["LIBRARIES"].value = libraries

            //create the HttpPost
            HttpPost post = new HttpPost(url)
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
            Helper.buildPostRequest(builder, id, name) {

                if (!inputs) throw new RuntimeException("No input files are specified")

                //process the options
                println "Submitting options: ${options}"
                options.each { Map.Entry<String, AnalysisOption> entry ->
                    String optionId = entry.key.toUpperCase()
                    AnalysisOption option = entry.value
                    if (option.value) {
                        if (optionId == "INPUTS") {
                            option.value.each { String jar ->
                                try {
                                    Helper.addFilesToMultiPart(optionId, [new File(jar)], builder)
                                }
                                catch(e) {
                                    //jar is not a local file
                                    Logger.getRootLogger().warn("$jar is not a local file, it will be posted as string.")
                                    builder.addPart("INPUTS", new StringBody(jar))
                                }
                            }
                        }
                        if (optionId == "LIBRARIES") {
                            option.value.each { String jar ->
                                try {
                                    Helper.addFilesToMultiPart(optionId, [new File(jar)], builder)
                                }
                                catch(e) {
                                    //jar is not a local file
                                    Logger.getRootLogger().warn("$jar is not a local file, it will be posted as string.")
                                    builder.addPart("LIBRARIES", new StringBody(jar))
                                }
                            }
                        }
                        else if (optionId == "DYNAMIC") {
                            Helper.addFilesToMultiPart(optionId, [new File(option.value)], builder)
                        }
                        else if (Helper.isFileOption(optionId)) {
                            Helper.addFilesToMultiPart(optionId, [new File(option.value)], builder)
                        }
                        else {
                            builder.addPart(optionId, new StringBody(option.value as String))
                        }
                    }
                }
            }
            HttpEntity entity = builder.build()
            post.setEntity(entity)
            return post
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return "Analysis posted: ${json.id}"
        }
    )


    /**
     * Posts to the analyses/family/cclyzer endpoint, printing the result.     
     */
    private static final CliRestCommand POST_CCLYZER = new CliRestCommand(
        name: 'post_cclyzer',
        description: "Posts a new cclyzer analysis to the remote server",
        endPoint: "analyses/family/cclyzer",
        options: [],
        requestBuilder: {String url ->
            throw new RuntimeException("Not supported yet")
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return "Analysis posted: ${json.id}"
        }
    )


    /**
     * Consumes the GET /analyses/[analysis-id] response, printing the result.
     */
    private static final CliRestCommand GET = new CliRestCommand(
        name: 'get',
        description: "Gets the analysis from the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpGet("${url}/${id}")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return processAnalysisData(null, json.analysis)
        }
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]/action/start response, printing the result.     
     */
    private static final CliRestCommand START = new CliRestCommand(
        name:'start',
        description: "Starts an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}/action/start")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]/action/stop response, printing the result.
     */
    private static final CliRestCommand STOP = new CliRestCommand(
        name: 'stop',
        description: "Stops an analysis running on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}/action/stop")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]/action/post_process response, printing the result.
     * TODO: This offers a convenience for testing
     */
    private static final CliRestCommand POST_PROCESS = new CliRestCommand(
        name: 'post_process',
        description: "Post processes an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}/action/post_process")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]/action/reset response, printing the result.
     * TODO: This offers a convenience for testing
     */
    private static final CliRestCommand RESET = new CliRestCommand(
        name: 'reset',
        description: "Resets an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}/action/reset")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]/action/restart response, printing the result.
     * TODO: This offers a convenience for testing
     */
    private static final CliRestCommand RESTART = new CliRestCommand(
        name: 'restart',
        description: "Restarts an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}/action/restart")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the POST /analyses/[analysis-id]/jcPluginMetadata response, printing the result.
     * {@see server.web.restlet.App, server.web.restlet.api.UploadJCPluginMetdataResource}
     * TODO: This offers a convenience for testing
     */
//    private static final CliRestCommand JC_PLUGIN_METADATA = new CliRestCommand(
//        name: 'jcplugin',
//        description: "Upload the jcplugin metadata zip file",
//        endPoint: "analyses",
//        options:[
//            ID,
//            OptionBuilder.hasArg().withArgName('jcplugin metadata zip file').
//                    withDescription('Upload the jc plugin metadata zip file').create('zip'),
//        ],
//        requestBuilder: {String url ->
//            if (cliOptions.id && cliOptions.zip) {
//                String id = cliOptions.id
//                String zip = cliOptions.zip
//                File zipFile = FileOps.findFileOrThrow(zip as String, "Not a valid file: $zip")
//
//                HttpPost post = new HttpPost("${url}/${id}/jcPluginMetadata")
//                MultipartEntityBuilder builder = MultipartEntityBuilder.create()
//                Helper.addFilesToMultiPart("jcPluginMetadata", [zipFile], builder)
//                post.setEntity(builder.build())
//
//                return post
//            }
//            else {
//                throw new RuntimeException("The id option is not specified")
//            }
//        },
//        authenticator: DEFAULT_AUTHENTICATOR,
//        onSuccess: DEFAULT_SUCCES
//    )

    /**
     * Consumes the DELETE /analyses/[analysis-id] response.
     */
    private static final CliRestCommand DELETE = new CliRestCommand(
        name:'delete',
        description: 'Delete the analysis from the remote server',
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpDelete("${url}/${id}")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Experimental - Search Maven Central and create doop.properties file(s) for the selected projects.
     */
    private static final CliRestCommand SEARCH_MAVEN = new CliRestCommand(
        name:'mvnsearch',
        description: 'Search Maven Central and create doop.properties files(s) for the selected projects.',
        authenticationRequired: false,
        options:[
            OptionBuilder.hasArg().withArgName('free text').withDescription('the search text').create('text')
        ],
        requestBuilder: {String url ->
            if (cliOptions.text) {
                String text = cliOptions.text
                return new HttpGet("http://search.maven.org/solrsearch/select?q=$text&rows=20&wt=json")
            }
            else {
                throw new RuntimeException("The text option is not specified")
            }
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")

            File dir = new File("mvn")
            dir.mkdirs()

            StringBuilder sb = new StringBuilder()
            sb.append("total:").append(json.response.numFound).append("\n")
            json.response.docs.eachWithIndex { doc, index ->

                File propsFile = new File(dir, "${doc.g}_${doc.a}_${doc.latestVersion}.properties")
                Properties props = new Properties()
                props.setProperty("analysis", "context-insensitive")
                props.setProperty("INPUTS", "${doc.g}:${doc.a}:${doc.latestVersion}")
                props.setProperty("allow_phantom", "true")

                new FileWriter(propsFile).withWriter { Writer w ->
                    props.store(w, null)
                }

                sb.append(index).append(".").
                    append(doc.g).append(":").append(doc.a).append(":").append(doc.latestVersion).
                    append(" -> ").append(propsFile).append("\n")
            }
            return sb.toString()
        }

    )

    /**
     * Consumes the GET /quickstart response, printing the result.
     */
    private static final CliRestCommand QUICKSTART = new CliRestCommand(
        name: 'quickstart',
        description: "Gets the quickstart guide for an analysis from the server.",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpGet("${url}/${id}/quickstart")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return json.results
        }
    )

    /**
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        PING, LOGIN, LIST_BUNDLES, POST_DOOP_BUNDLE, POST_DOOP, POST_CCLYZER, LIST, GET, START, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART
    ].collectEntries {
        [(it.name):it]
    }
}
