package org.clyze.client.web

//import groovy.transform.TypeChecked
import java.awt.*
import java.util.List
import org.apache.commons.cli.Option
import org.apache.http.HttpEntity
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.clyze.client.Message
import org.clyze.client.web.api.AttachmentHandler
import org.clyze.client.web.api.LowLevelAPI
import org.clyze.client.web.api.Remote
import org.clyze.client.web.http.HttpClientCommand
import org.clyze.client.web.http.HttpClientLifeCycle

//@TypeChecked
class Helper {

    /** Default file to record metadata when posting a bundle. */
    public static final String POST_METADATA       = "post-metadata.json"

    final static String ANALYSIS_JSON = "analysis.json"

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

//    private static HttpClientCommand<String> createAutoLoginTokenCommand(Closure authenticator) {
//        return new HttpClientCommand<String>(
//            endPoint: "token",
//            requestBuilder:  { String url ->
//                return new HttpPost(url)
//            },
//            onSuccess: { HttpEntity entity ->
//                def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
//                return json.token
//            },
//            authenticator: authenticator
//        )
//    }

    static HttpClientCommand<Object> createCommandForOptionsDiscovery(String what, HttpClientLifeCycle httpClientLifeCycle) {     

        new HttpClientCommand(
            httpClientLifeCycle: httpClientLifeCycle,
            requestBuilder     : { String host, int port ->
                if (what == 'BUNDLE') {
                    LowLevelAPI.Requests.getProfileOptions(host, port)
                } else {
                    throw new RuntimeException("Cannot find options for: ${what}")
                }
            },
            onSuccess          : { HttpEntity entity ->
                LowLevelAPI.Responses.parseJson(entity)
            }
        )            
    }

    static List<Option> convertJsonEncodedOptionsToCliOptions(Object json) {
        if (!json?.results) {
            return []
        }
        List<Option> ret = new LinkedList<>()
        json.results.each { result ->
            List<Option> opts = result.options.collect { option ->
                String description = option.description
                if (!description) {
                    description = "<no description>"
                }
                if (option.validValues) {
                    description = "${description}\nAllowed values: ${option.validValues.join(', ')}"
                }
                if (option.defaultValue) {
                    description = "${description}\nDefault value: ${option.defaultValue}"
                }
                if (option.isMandatory) {
                    description = "${description}\nMandatory option."
                }
                if (option.multipleValues) {
                    description = "${description}\nRepeatable option."
                }

                Option o = new Option(null, option.id?.toLowerCase(), !option.isBoolean, description)
                if (option.multipleValues) {
                    o.setArgs(Option.UNLIMITED_VALUES)
                    if (option.isFile) {
                        o.setArgName("files")
                    }
                } else if (option.isFile) {
                    o.setArgName("file")
                }
                return o
            }
            ret.addAll(opts)
        }
        return ret
    }     

    public static Closure<Boolean> checkFileEmpty = { String f ->
        boolean isEmpty = (new File(f)).length() == 0
        if (isEmpty) {
            println "Skipping empty file ${f}"
        }
        !isEmpty
    }

    static Remote connect(String host, int port, String username, String password) throws HttpHostConnectException {
        println "Connecting to ${host}:${port}"
        Remote remote = Remote.at(host, port)

        println "Logging in as ${username}"
        remote.login(username, password)

        return remote
    }

    /**
     * Check if the project exists and attempt to create it if it is missing.
     *
     * @param remote        the Remote object to use for the connection to the server
     * @param projectName   the project name
     * @param platform      the project platform
     */
    static void ensureProjectExists(Remote remote, String projectName, String platform) {
        if (!projectName)
            throw new RuntimeException("Missing project name")
        else if (!platform)
            throw new RuntimeException("Missing project platform")

        try {
            remote.getProject(remote.currentUser(), projectName)
        } catch (Exception ex1) {
            try {
                remote.createProject(projectName, platform)
                println "Project '${projectName}' created."
            } catch (Exception ex2) {
                throw new RuntimeException("Could not create project '${projectName}'.", ex2)
            }
        }
    }

    /**
     * Invokes the automated repackaging endpoint.
     *
     * @param host         the server host name
     * @param port         the server port
     * @param username     the user name
     * @param password     the user password
     * @param projectName  the project to post the bundle
     * @param platform     the project platform (Android/Java)
     * @param ps           the bundle representation
     * @param handler      a handler of the resulting file returned by the server
     * @throws ClientProtocolException  if the server encountered an error
     */
    static void repackageBundleForCI(String host, int port, String username, String password,
                                     String projectName, String platform, PostState ps,
                                     AttachmentHandler<String> handler)
    throws ClientProtocolException {
        Remote remote = connect(host, port, username, password)
        ensureProjectExists(remote, projectName, platform)
        remote.repackageBundleForCI(username, projectName, ps, handler)
    }

    /**
     * Invokes the endpoint that creates/posts a bundle.
     *
     * @param host              the server host name
     * @param port              the server port
     * @param username          the user name
     * @param password          the user password
     * @param projectName       the project to post the bundle
     * @param platform          the project platform (Android/Java)
     * @param profile           the profile to use in the server
     * @param bundlePostState   the bundle object
     */
    static void postBundle(String host, int port, String username, String password,
                           String projectName, String platform, String profile, PostState bundlePostState)
    throws HttpHostConnectException, ClientProtocolException {
        Remote remote = connect(host, port, username, password)

        ensureProjectExists(remote, projectName, platform)

        println "Submitting bundle in project '${projectName}'..."
        String bundleId = remote.createBundle(username, projectName, profile, bundlePostState)
        println "Done (new bundle $bundleId)."
    }

    static void post(PostState ps, PostOptions options, List<Message> messages,
                     String cachePostDir, File metadataDir, boolean debug = false) {
        // Optional: save state that will be uploaded.
        if (cachePostDir != null) {
            try {
                File tmpDir = new File(cachePostDir)
                tmpDir.mkdirs()
                ps.saveTo(tmpDir)
                Message.print(messages, "Saved post state in " + cachePostDir)
            } catch (Exception ex) {
                Message.warn(messages, "WARNING: Cannot save post state: " + ex.getMessage())
                if (debug)
                    ex.printStackTrace()
            }
        }

        // Optional: save post request options.
        if (metadataDir != null) {
            File metadataFile = new File(metadataDir, POST_METADATA)
            new BufferedWriter(new FileWriter(metadataFile)).withCloseable { BufferedWriter writer ->
                try {
                    Message.debug(messages, "Saving options in: " + metadataFile.getCanonicalPath())
                    writer.write(ps.toJSONWithRelativePaths(metadataDir.getCanonicalPath()))
                } catch (IOException ex) {
                    Message.warn(messages, "WARNING: Cannot save metadata: " + ex.getMessage())
                    if (debug)
                        ex.printStackTrace()
                }
            }
        }

        try {
            if (!isServerCapable(options, messages))
                return

            if (!options.dry)
                postBundle(options.host, options.port, options.username,
                           options.password, options.project, options.platform,
                           options.profile, ps)
        } catch (HttpHostConnectException ex) {
            Message.print(messages, "ERROR: Cannot post bundle, is the server running?")
            if (debug)
                ex.printStackTrace()
        } catch (Exception ex) {
            Message.print(messages, "ERROR: Cannot post bundle: " + ex.getMessage())
            if (debug)
                ex.printStackTrace()
        }
    }

    /**
     * Test server capabilities.
     *
     * @param options     the post options to use
     * @param messages    a list of messages to contain resulting errors/warnings
     * @return            true if the server is compatible, false otherwise (see
     *                    messages for reason)
     * @throws HttpHostConnectException if the server did not respond
     */
    static boolean isServerCapable(PostOptions options, List<Message> messages)
        throws HttpHostConnectException {

        if (options.dry)
            return true

        Map<String, Object> diag = diagnose(options)
        // Check if the server can receive Android bundles.
        if (options.android && !isAndroidSupported(diag)) {
            Message.print(messages, "ERROR: Cannot post bundle: Android SDK setup missing.")
            return false
        } else if (options.autoRepackaging && !supportsAutomatedRepackaging(diag)) {
            Message.print(messages, "ERROR: This version of the server does not support automated repackaging.")
            return false
        } else {
            String sv = getServerVersion(diag)
            String expected = "1.0.3"
            if (sv != expected)
                Message.warn(messages, "WARNING: Server version not compatible: " + sv + " (expected: " + expected + ")")
        }
        return true
    }

    /**
     * Helper method to check if the "diagnose" output of the server supports
     * posting of Android apps.
     *
     * @param diag   the JSON output of the server endpoint (as a Map)
     * @return       true if the server supports Android apps, false otherwise
     */
    static boolean isAndroidSupported(Map<String, Object> diag) {
        Boolean androidSDK_OK = (Boolean)diag.get("ANDROID_SDK_OK")
        return (androidSDK_OK == null) || androidSDK_OK
    }

    /**
     * Returns the server version.
     *
     * @param   diag the output of the 'diagnose' endpoint
     * @return  the contents of the server version field
     */
    static String getServerVersion(Map<String, Object> diag) {
        return (String)diag.get("SERVER_VERSION")
    }

    /**
     * Checks if the server supports automated repackaging.
     *
     * @param   diag the output of the 'diagnose' endpoint
     * @return  true if the server supports automated repackaging
     */
    static boolean supportsAutomatedRepackaging(Map<String, Object> diag) {
        return (Boolean)diag.get("AUTOMATED_REPACKAGING")
    }

    /**
     * Invokes the "diagnose" endpoint of the server and returns its response.
     *
     * @param options     the post options to use
     * @return the JSON response as a Map
     * @throws HttpHostConnectException if the server did not respond
     */
    static Map<String, Object> diagnose(PostOptions options) throws HttpHostConnectException {
        return Remote.at(options.host, options.port).diagnose()
    }

    static void postCachedBundle(PostOptions options, File fromDir,
                                 String bundleId, List<Message> messages,
                                 boolean debug) {
        PostState bundlePostState
        try {
            // Check if a bundle post state exists.
            bundlePostState = new PostState(id: bundleId)
            bundlePostState.loadAndTranslatePathsFrom(fromDir)
        } catch (any) {
            Message.print(messages, "Error bundling state: ${any.message}" as String)
            return
        }

        post(bundlePostState, options, messages, null, null, debug)
    }
}
