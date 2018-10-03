package org.clyze.client.web

import groovy.json.JsonSlurper

import org.clyze.client.web.http.HttpClientCommand

//import groovy.transform.TypeChecked
import org.apache.commons.cli.Option
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import org.clyze.analysis.AnalysisFamilies
import org.clyze.analysis.AnalysisOption
import org.clyze.doop.core.Doop
import org.clyze.doop.core.DoopAnalysisFamily
import org.clyze.doop.input.*

import java.awt.*
import java.nio.file.attribute.PosixFilePermission
import java.util.List

import static java.nio.file.Files.setPosixFilePermissions

//@TypeChecked
class Helper {

    final static String ANALYSIS_JSON = "analysis.json"

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
        files?.each { File f ->
            if (f != null) { builder.addPart(name, new FileBody(f)) }
        }
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
    static HttpClientCommand<String> createLoginCommand(String username, String password) {
        return new HttpClientCommand<String>(
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
     * Post an analysis (represented by argument 'ps') to the
     * server. If flag 'cache' is set, the analysis is also cached to
     * be replayed later.
     */
    public static void postAndStartAnalysis(PostState ps, boolean cache, boolean dry) {

        // Optionally save the data to be posted to the server, so
        // that they can be reused in the future.
        if (cache) {
            String tmpDir = java.nio.file.Files.createTempDirectory("").toString()

            def copyToTmp = { String fPath ->
                String fName = fPath.substring(fPath.lastIndexOf(File.separator))
                File newFile = new File("${tmpDir}/${fName}")
                newFile << (new File(fPath)).bytes
                newFile
            }

            // Replace all file paths with the paths of their copies.
            def copier = { copyToTmp(it).canonicalPath }
            ps.options.inputs    = ps.options.inputs.collect    copier
            ps.options.libraries = ps.options.libraries.collect copier
            ps.options.heapDLs    = ps.options.heapDLs.collect    copier
            if (ps.sources          != null) { ps.sources          = copyToTmp(ps.sources.canonicalPath)          }
            if (ps.jcPluginMetadata != null) { ps.jcPluginMetadata = copyToTmp(ps.jcPluginMetadata.canonicalPath) }

            // Save remaining information.
            new File("${tmpDir}/${ANALYSIS_JSON}") << ps.toJson()

            // Generate optional script to call Doop.
            File doopScript = new File("${tmpDir}/run-doop.sh")
            doopScript << ps.generateDoopScript(tmpDir)
            // Try to make file executable.
            try {
                Set<PosixFilePermission> perms =
                    [ PosixFilePermission.OWNER_READ,
                      PosixFilePermission.OWNER_WRITE,
                      PosixFilePermission.OWNER_EXECUTE,
                      PosixFilePermission.GROUP_READ,
                      PosixFilePermission.GROUP_WRITE,
                      PosixFilePermission.GROUP_EXECUTE,
                      PosixFilePermission.OTHERS_READ,
                      PosixFilePermission.OTHERS_EXECUTE ] as Set
                setPosixFilePermissions(doopScript.toPath(), perms)
            } catch (Exception ex) {
                println ("Failed to make ${doopScript} executable: " + ex.getClass().getName())
            }

            println "Analysis submission data saved in ${tmpDir}"
        }

        if (dry) {
            println "End of dry run."
            return
        }

        println "Connecting to server at ${ps.host}:${ps.port}"
        String token = createLoginCommand(ps.username, ps.password).execute(ps.host, ps.port)

        println "Submitting ${ps.projectName} version ${ps.projectVersion} for ${ps.options.analysis} analysis"

        def authenticator = {String h, int p, HttpUriRequest request ->
            //send the token with the request
            request.addHeader(HttpClientCommand.HEADER_TOKEN, token)
        }

        String autoLoginToken = null

        HttpClientCommand<String> postAnalysis = createPostDoopAnalysisCommand(
            ps.orgName,
            ps.projectName,
            ps.projectVersion,
            null, //rating
            null, //ratingCount
            ps.sources,
            ps.jcPluginMetadata,
            ps.heapDLs,
            ps.options
        )
        postAnalysis.authenticator = authenticator

        String postedId = postAnalysis.execute(ps.host, ps.port).replaceAll("\\+", "%2b")
        println "The analysis has been submitted successfully: $postedId."

        HttpClientCommand<String> createAutoLoginToken = createAutoLoginTokenCommand(authenticator)
        try {
            autoLoginToken = createAutoLoginToken.execute(ps.host, ps.port)
        }
        catch(Exception e) {
            println "Autologin failed: ${e.getMessage()}"
        }

        String analysisPageURL = createAnalysisPageURL(ps.host, ps.port, postedId, autoLoginToken)

        HttpClientCommand<Void> start = createStartCommandAuth(postedId, authenticator)
        start.onSuccess = { HttpEntity ent ->

            if (autoLoginToken) {
                println "Sit back and relax while we analyze your code..."
                try {
                    openBrowser(analysisPageURL)
                } catch(Exception e) {
                    println "Analysis has been posted to the server, please visit $analysisPageURL"
                }
            }
            else {
                println "Visit $analysisPageURL"
            }
        }
        start.execute(ps.host, ps.port)
    }

    /**
     * Creates a post doop analysis command (without authenticator) that returns the id of the newly created doop analysis.
     */
    static HttpClientCommand<String> createPostDoopAnalysisCommand(String orgName,
                                                                 String projectName,
                                                                 String projectVersion,
                                                                 String rating,
                                                                 String ratingCount,
                                                                 File sources,
                                                                 File jcPluginMetadata,
                                                                 List<File> heapDLs,
                                                                 Map<String, Object> options) {
        return new HttpClientCommand<String>(
            endPoint: "analyses/family/doop",
            requestBuilder: { String url ->
                HttpPost post = new HttpPost(url)
                MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                //submit a null id for the analysis to make the server generate one automatically
                buildPostRequest(builder, null, options.analysis) {

                    //process the org, project name and version as well as rating info
                    if (orgName) builder.addPart("orgName", new StringBody(orgName))
                    if (projectName) builder.addPart("projectName", new StringBody(projectName))
                    if (projectVersion) builder.addPart("projectVersion", new StringBody(projectVersion))
                    if (rating) builder.addPart("rating", new StringBody(rating))
                    if (ratingCount) builder.addPart("ratingCount", new StringBody(ratingCount))

                    //process the sources
                    println "Submitting sources: ${sources}"
                    addFilesToMultiPart("sources", [sources], builder)

                    //process the jcPluginMetadata
                    println "Submitting jcplugin metadata: ${jcPluginMetadata}"
                    addFilesToMultiPart("jcPluginMetadata", [jcPluginMetadata], builder)

                    //process the HeapDL files
                    if ((heapDLs != null) && (heapDLs.size() > 0)) {
                        println "Submitting HeapDL inputs: ${heapDLs}"
                        addFilesToMultiPart("HEAPDL", heapDLs, builder)
                    }

                    // Process the options.
                    println "Submitting options: ${options}"
                    // Convert the inputs from a string list to a file list.
                    if (options.inputs != null) {
                        options.inputs = options.inputs.collect { new File(it) }
                    }
                    if (options.libraries != null) {
                        options.libraries = options.libraries.collect { new File(it) }
                    }
                    options.each { Map.Entry<String, Object> entry ->
                        String optionId = entry.key.toUpperCase()
                        def value = entry.value
                        if (value) {
                            if (optionId == "INPUTS" || optionId == "LIBRARIES" ||
                                optionId == "DYNAMIC" || optionId == "HEAPDL") {
                                addFilesToMultiPart(optionId, value, builder)
                            }
                            else if (isFileOption(optionId)) {
                                addFilesToMultiPart(optionId, [new File(value)], builder)
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

    private static String createAnalysisPageURL(String host, int port, String postedId, String token = null) {
        return "http://$host:$port/clue/" + (token ? "?t=$token" : "") + "#/analyses/$postedId"
    }

    private static void openBrowser(String url) {
        File html = File.createTempFile("_doop", ".html")
        html.withWriter('UTF-8') { w ->
            w.write """\
                    <html>
                        <head>
                            <script>
                                document.location="$url"
                            </script>
                        </head>
                        <body>
                        </body>
                    </html>
                    """.stripIndent()
        }
        Desktop.getDesktop().browse(html.toURI())
    }

    /**
     * Creates a start analysis command (without authenticator and onSuccess handlers).
     */
    static HttpClientCommand<Void> createStartCommand(String id) {
        return new HttpClientCommand<Void>(
            endPoint: "analyses",
            requestBuilder: {String url ->
                return new HttpPut("${url}/${id}/action/start")
            }
        )
    }

    /**
     * Creates a start analysis command (with authenticator).
     */
    private static HttpClientCommand<Void> createStartCommandAuth(String id, Closure authenticator) {
        HttpClientCommand<Void> command = createStartCommand(id)
        command.authenticator = authenticator
        return command
    }

    private static HttpClientCommand<String> createAutoLoginTokenCommand(Closure authenticator) {
        return new HttpClientCommand<String>(
            endPoint: "token",
            requestBuilder:  { String url ->
                return new HttpPost(url)
            },
            onSuccess: { HttpEntity entity ->
                def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
                return json.token
            },
            authenticator: authenticator
        )
    }

    /**
     * Post again a cached analysis input.
     */
    public static void replayPost(String path) {
        println "Reading state from ${path}..."
        PostState ps = PostState.fromJson(path, ANALYSIS_JSON)

        // Optionally read properties from ~/.gradle/gradle.properties.
        String homeDir = System.getProperty("user.home")
        if (homeDir != null) {
            String propertiesFileName = "${homeDir}/.gradle/gradle.properties"
            File propertiesFile = new File(propertiesFileName)
            if (propertiesFile.exists()) {
                println "Reading connection information from ${propertiesFile.getCanonicalPath()}"
                Properties properties = new Properties()
                propertiesFile.withInputStream {
                    properties.load(it)
                }
                def readProperty = { String name ->
                    String readVal = properties.getProperty(name)
                    if (readVal != null) {
                        println "Found ${name} = ${readVal}"
                    }
                    readVal
                }
                ps.username = readProperty("clue_user")             ?: ps.username
                ps.password = readProperty("clue_password")         ?: ps.password
                ps.host     = readProperty("clue_host")             ?: ps.host
                ps.port     = readProperty("clue_port").toInteger() ?: ps.port
            }
        }

        // Turn off caching, we already have a cache available.
        boolean cache = false

        postAndStartAnalysis(ps, cache, false)
    }

    static HttpClientCommand<List> createCommandForOptionsDiscovery(String whatOptions) {
        return new HttpClientCommand<List>(
            endPoint: "options",
            authenticationRequired: false,
            requestBuilder: { String url ->            
                return new HttpGet("${url}?what=${whatOptions}")
            },
            onSuccess: { HttpEntity entity ->
                def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
                return json.options //the options array
            }
        )
    }

    static List<Option> convertJsonEncodedOptionsToCliOptions(List<Object> jsonList) {
        jsonList.collect { option ->
            String description = option.description
            if (option.validValues) {
                description = "${description} (Allowed values: ${option.validValues.join(', ')})"
            }
            Option o = new Option(null, option.label, true, description)            
            if (option.multipleValues) {
                o.setArgs(Option.UNLIMITED_VALUES)
                if (option.isFile) {
                    o.setArgName("files")    
                }
            }
            else {
                if (option.isFile) {
                    o.setArgName("file")
                }
            }            
            return o
        }
    }    
}
