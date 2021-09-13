package com.clyze.client.web

import com.clyze.client.Printer
import com.clyze.client.web.api.AttachmentHandler
import com.clyze.client.web.api.Remote
import groovy.transform.CompileStatic
import java.awt.*
import java.util.List
import org.clyze.persistent.metadata.JSONUtil
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

@SuppressWarnings('unused')
@CompileStatic
class Helper {

    /** Default file to record metadata when posting a snapshot. */
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

    private static void openBrowser(String url) {
        File html = File.createTempFile("_clyze", ".html")
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

    public static Closure<Boolean> checkFileEmpty = { String f ->
        boolean isEmpty = (new File(f)).length() == 0
        if (isEmpty) {
            println "Skipping empty file ${f}"
        }
        !isEmpty
    }

    /**
     * Connects to the server.
     * @param hostPrefix   the server host prefix (host name, port, base path)
     * @param username     the user name
     * @param token        the authentication token to use
     * @param tokenType    if true, this is a JWT token; if false, token is a password
     * @param printer      the printer to use for printing messages
     * @return             the Remote object to use for interacting with the server
     * @throws HttpHostConnectException    on connection failures
     */
    static Remote connect(String hostPrefix, String username, String token, boolean tokenType,
                          Printer printer) throws HttpHostConnectException {
        printer.always("Connecting to ${hostPrefix} as ${username}")
        Remote remote = Remote.at(hostPrefix, username, tokenType ? token : null)

        if (!tokenType) {
            printer.always("Logging in as ${username} (with password)")
            remote.login(username, token)
        }

        return remote
    }

    /**
     * Check if the project exists and attempt to create it if it is missing.
     *
     * @param remote        the Remote object to use for the connection to the server
     * @param projectName   the project name
     * @param stacks        the project stacks
     * @param printer       receiver of messages to display
     * @param makePublic    if creating a new project, make it public
     * @param debug         debugging mode
     */
    static void ensureProjectExists(Remote remote, String projectName,
                                    List<String> stacks, Printer printer,
                                    boolean makePublic, boolean debug) {
        if (!projectName)
            throw new RuntimeException("Missing project name")
        else if (!stacks)
            throw new RuntimeException("Missing project stacks")

        Map<String, Object> proj
        try {
            proj = remote.getProject(remote.currentUser(), projectName)
        } catch (Exception ex1) {
            if (debug)
                ex1.printStackTrace()
            try {
                proj = remote.createProject(remote.currentUser(), projectName, stacks, makePublic ? 'true' : 'false')
                printer.always("Project '${projectName}' created with stacks: ${stacks}")
            } catch (Exception ex2) {
                throw new RuntimeException("Could not create project '${projectName}'.", ex2)
            }
        }
        if (debug)
            printer.always("Project data: ${JSONUtil.objectWriter.writeValueAsString(proj)}")
    }

    /**
     * Invokes the automated repackaging endpoint.
     *
     * @param hostPrefix   the server host prefix (host name, port, base path)
     * @param username     the user name
     * @param password     the user password
     * @param projectName  the project to post the snapshot
     * @param stacks       the project platform (Android/Java)
     * @param ps           the snapshot representation
     * @param handler      a handler of the resulting file returned by the server
     * @param printer      receiver of messages to display
     * @throws ClientProtocolException  if the server encountered an error
     */
    @SuppressWarnings('unused')
    static void repackageSnapshotForCI(String hostPrefix, String username,
                                       String password, String projectName, PostState ps,
                                       AttachmentHandler<String> handler, Printer printer)
    throws ClientProtocolException {
        Remote remote = connect(hostPrefix, username, password, true, printer)
        ensureProjectExists(remote, projectName, ps.stacks, printer, ps.makePublic, false)
        remote.repackageSnapshotForCI(username, projectName, ps, handler)
    }

    /**
     * Invokes the endpoint that creates/posts a snapshot.
     *
     * @param hostPrefix        the server host prefix (host name, port, base path)
     * @param username          the user name
     * @param token             the user authentication token
     * @param projectName       the project to post the snapshot
     * @param ps                the snapshot object
     * @param printer           receiver of messages to display
     * @param debug             debugging mode
     */
    static void postSnapshot(String hostPrefix, String username, String token,
                             String projectName, PostState ps,
                             Printer printer, boolean debug)
    throws HttpHostConnectException, ClientProtocolException {
        Remote remote = connect(hostPrefix, username, token, true, printer)

        ensureProjectExists(remote, projectName, ps.stacks, printer, ps.makePublic, debug)

        printer.always("Posting snapshot to project '${projectName}'...")
        String snapshotId = remote.createSnapshot(username, projectName, ps)
        if (debug)
            printer.always("Done (new snapshot $snapshotId).")
        else
            printer.always("Done.")
    }

    static void post(PostState ps, PostOptions options, File cachePostDir,
                     File metadataDir, Printer printer, boolean debug) {
        // Optional: save state that will be uploaded.
        if (cachePostDir != null) {
            try {
                cachePostDir.mkdirs()
                ps.saveTo(cachePostDir)
                printer.always("Saved post state in " + cachePostDir.canonicalPath)
            } catch (Exception ex) {
                printer.warn("WARNING: Cannot save post state: " + ex.getMessage())
                if (debug)
                    ex.printStackTrace()
            }
        }

        // Optional: save post request options.
        if (metadataDir != null) {
            File metadataFile = new File(metadataDir, POST_METADATA)
            new BufferedWriter(new FileWriter(metadataFile)).withCloseable { BufferedWriter writer ->
                try {
                    printer.debug("Saving options in: " + metadataFile.getCanonicalPath())
                    writer.write(ps.toJSONWithRelativePaths(metadataDir.getCanonicalPath()))
                } catch (IOException ex) {
                    printer.warn("WARNING: Cannot save metadata: " + ex.getMessage())
                    if (debug)
                        ex.printStackTrace()
                }
            }
        }

        try {
            if (!isServerCapable(options, printer))
                return

            if (!options.dry)
                postSnapshot(options.getHostPrefix(),
                        options.username, options.password, options.project, ps, printer, debug)
        } catch (HttpHostConnectException ex) {
            printer.error("ERROR: Cannot post snapshot, is the server running?")
            if (debug)
                ex.printStackTrace()
        } catch (Exception ex) {
            printer.error("ERROR: Cannot post snapshot: " + ex.getMessage())
            if (debug)
                ex.printStackTrace()
        }
    }

    /**
     * Test server capabilities.
     *
     * @param options     the post options to use
     * @param printer     receiver of messages to display
     * @return            true if the server is compatible, false otherwise (see
     *                    messages for reason)
     * @throws HttpHostConnectException if the server did not respond
     */
    static boolean isServerCapable(PostOptions options, Printer printer)
        throws HttpHostConnectException {

        if (options.dry)
            return true

        Map<String, Object> diag = diagnose(options)
        // Check if the server can receive Android snapshots.
        if (options.android && !isAndroidSupported(diag)) {
            printer.error("ERROR: Cannot post snapshot: Android SDK setup missing.")
            return false
        } else if (options.autoRepackaging && !supportsAutomatedRepackaging(diag)) {
            printer.error("ERROR: This version of the server does not support automated repackaging.")
            return false
        } else {
            String sv = getServerVersion(diag)
            String expected = "1.0.3"
            if (sv != expected)
                printer.warn("WARNING: Server version not compatible: " + sv + " (expected: " + expected + ")")
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
        return Remote.at(options.getHostPrefix(), null, null).diagnose()
    }

    static void postCachedSnapshot(PostOptions options, File fromDir,
                                   String snapshotId, Printer printer,
                                   boolean debug) {
        PostState snapshotPostState
        try {
            // Check if a snapshot post state exists.
            snapshotPostState = new PostState(id: snapshotId)
            snapshotPostState.loadAndTranslatePathsFrom(fromDir)
            snapshotPostState.stacks = options.stacks
        } catch (any) {
            printer.error("Error bundling state: ${any.message}" as String)
            return
        }

        post(snapshotPostState, options, null, null, printer, debug)
    }
}
