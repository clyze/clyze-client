package org.clyze.client.web

//import groovy.transform.TypeChecked
import java.awt.*
import java.util.List
import org.apache.commons.cli.Option
import org.apache.http.HttpEntity
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.clyze.client.web.api.LowLevelAPI
import org.clyze.client.web.api.Remote
import org.clyze.client.web.http.HttpClientCommand
import org.clyze.client.web.http.HttpClientLifeCycle

//@TypeChecked
class Helper {

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

//    /**
//     * Creates a start analysis command (without authenticator and onSuccess handlers).
//     */
//    static HttpClientCommand<Void> createStartCommand(String id) {
//        return new HttpClientCommand<Void>(
//            endPoint: "analyses",
//            requestBuilder: {String url ->
//                return new HttpPut("${url}/${id}/action/start")
//            }
//        )
//    }

//    /**
//     * Creates a start analysis command (with authenticator).
//     */
//    private static HttpClientCommand<Void> createStartCommandAuth(String id, Closure authenticator) {
//        HttpClientCommand<Void> command = createStartCommand(id)
//        command.authenticator = authenticator
//        return command
//    }

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
     */
    static void ensureProjectExists(Remote remote, String projectName) {
        if (!projectName)
            throw new RuntimeException("Missing project name")

        try {
            remote.getProject(remote.currentUser(), projectName)
        } catch (Exception ex1) {
            try {
                remote.createProject(projectName)
                println "Project '${projectName}' created."
            } catch (Exception ex2) {
                throw new RuntimeException("Could not create project '${projectName}'.", ex2)
            }
        }
    }

    static void doPost(String host, int port, String username, String password, String projectName, String profile, PostState bundlePostState) throws HttpHostConnectException {
        Remote remote = connect(host, port, username, password)

        ensureProjectExists(remote, projectName)

        println "Submitting bundle in project '${projectName}'..."
        String bundleId = remote.createBundle(username, projectName, profile, bundlePostState)
        println "Done (new bundle $bundleId)."
    }
}
