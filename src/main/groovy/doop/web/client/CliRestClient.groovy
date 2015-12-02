package doop.web.client

import doop.CommandLineAnalysisFactory
import doop.core.AnalysisOption
import doop.core.Doop
import doop.core.Helper
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
 *     <li>login  - authenticate user.
 *     <li>ping   - check connection with server.
 *     <li>list   - list the available analyses.
 *     <li>post   - create a new analysis.
 *     <li>get    - retrieves an analysis.
 *     <li>start  - start an analysis.
 *     <li>stop   - stop an analysis.
 *     <li>query  - query a complete analysis.
 *     <li>delete - delete an analysis.
 * </ul>
 *
 * Experimentally, the client also supports fetching all the jars from Maven Central that match a free-text query.
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 11/2/2015
 */
class CliRestClient {

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

    private static final String processAnalysisData(Integer index, def analysisData) {

        return """\
               ${index? "($index)":""} ${analysisData.id}
               Name  : ${analysisData.name}
               Jars  : ${analysisData.jars.join(", ")}
               Status: ${analysisData.state}""".stripIndent()
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
     * {@see doop.web.restlet.App, doop.web.restlet.Ping}
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
     * Consumes the GET /analyses response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysesResource}
     */
    private static final CliRestCommand LIST = new CliRestCommand(
        name: 'list',
        description: "Lists the analyses of the remote server",
        endPoint: "analyses",
        requestBuilder: DEFAULT_REQUEST_BUILDER,
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return doop.web.client.Helper.collectWithIndex(json.list) { def data, int i ->
                processAnalysisData(i, data)
            }.join("\n")
        }
    )

    /**
     * Posts to the /analyses endpoint, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysesResource}
     */
    private static final CliRestCommand POST = new CliRestCommand(
        name: 'post',
        description: "Posts a new analysis to the remote server",
        endPoint: "analyses",
        options: [
                OptionBuilder.withLongOpt('analysis').hasArg().withArgName('name').
                        withDescription(CommandLineAnalysisFactory.ANALYSIS).create('a'),
                OptionBuilder.withLongOpt('jar').hasArgs(Option.UNLIMITED_VALUES).withArgName('jar').
                        withDescription(CommandLineAnalysisFactory.JAR).withValueSeparator(',' as char).create('j'),
                OptionBuilder.withLongOpt('identifier').hasArg().withArgName('identifier').
                        withDescription(CommandLineAnalysisFactory.USER_SUPPLIED_ID).create('id'),
                OptionBuilder.withLongOpt('properties').hasArg().withArgName('properties').
                        withDescription(CommandLineAnalysisFactory.PROPS).create('p'),
        ] + Helper.convertAnalysisOptionsToCliOptions(Doop.ANALYSIS_OPTIONS.findAll { it.webUI }),
        requestBuilder: {String url ->

            String name, id
            List<String> jars
            Map<String, AnalysisOption> options

            if (cliOptions.p) {
                //load the analysis options from the property file
                String file = cliOptions.p
                File f = Helper.checkFileOrThrowException(file, "Not a valid file: $file")
                File propsBaseDir = f.getParentFile()
                Properties props = Helper.loadProperties(file)

                //Get the name of the analysis
                name = cliOptions.a ?: props.getProperty("analysis")

                //Get the jars of the analysis. If there are no jars in the CLI, we get them from the properties.
                jars = cliOptions.js
                if (!jars) {
                    jars = props.getProperty("jar").split().collect { String s -> s.trim() }
                    //The jars, if relative, are being resolved via the propsBaseDir
                    jars = jars.collect { String jar ->
                        File jarFile = new File(jar)
                        return jarFile.isAbsolute() ? jar : new File(propsBaseDir, jar).getCanonicalFile().getAbsolutePath()
                    }
                }

                //Get the optional id of the analysis
                id = cliOptions.id ?: props.getProperty("id")

                options = Doop.overrideDefaultOptionsWithProperties(props) {AnalysisOption option -> option.webUI }
                Doop.overrideOptionsWithCLI(options, cliOptions) {AnalysisOption option -> option.webUI }
            }
            else {

                //Get the name of the analysis
                name = cliOptions.a ?: null
                //Get the jars of the analysis
                jars = cliOptions.js ?: null
                //Get the optional id of the analysis
                id = cliOptions.id ?: null

                options = Doop.overrideDefaultOptionsWithCLI(cliOptions) { AnalysisOption option -> option.webUI }
            }

            //create the HttpPost
            HttpPost post = new HttpPost(url)
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
            doop.web.client.Helper.buildPostRequest(builder, id, name) {

                if (!jars) throw new RuntimeException("The jar option is not specified")

                //add the jars
                jars.each{ String jar ->
                    try {
                        doop.web.client.Helper.addFilesToMultiPart("jar", doop.web.client.Helper.resolveFiles([jar]), builder)
                    }
                    catch(e) {
                        //jar is not a local file
                        Logger.getRootLogger().warn("$jar is not a local file, it will be posted as string.")
                        builder.addPart("jar", new StringBody(jar))
                    }
                }

                //add the options
                options.each { Map.Entry<String, AnalysisOption> entry ->
                    String optionName = entry.getKey()
                    AnalysisOption option = entry.getValue()
                    if (option.value) {
                        if (optionName == "DYNAMIC") {
                            List<String> dynamicFiles = option.value as List<String>
                            doop.web.client.Helper.addFilesToMultiPart("DYNAMIC", doop.web.client.Helper.resolveFiles(dynamicFiles), builder)
                        } else if (option.isFile) {
                            doop.web.client.Helper.addFilesToMultiPart(optionName, doop.web.client.Helper.resolveFiles([option.value as String]), builder)
                        } else {
                            builder.addPart(optionName, new StringBody(option.value as String))
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
     * Consumes the GET /analyses/[analysis-id] response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
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
     * Consumes the PUT /analyses/[analysis-id]?status=start response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final CliRestCommand START = new CliRestCommand(
        name:'start',
        description: "Starts an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}?status=start")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]?status=stop response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final CliRestCommand STOP = new CliRestCommand(
        name: 'stop',
        description: "Stops an analysis running on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}?status=stop")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]?status=post_process response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     * TODO: This offers a convenience for testing,
     */
    private static final CliRestCommand POST_PROCESS = new CliRestCommand(
        name: 'post_process',
        description: "Post processes an analysis on the remote server",
        endPoint: "analyses",
        options:[ID],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}?status=post_process")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: DEFAULT_SUCCES
    )

    /**
     * Consumes the GET /analyses/[analysis-id]/query response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.QueryAnalysisResource}
     */
    private static final CliRestCommand QUERY = new CliRestCommand(
        name: 'query',
        description: "Queries an analysis that has completed on the remote server",
        endPoint: "analyses",
        options:[
            ID,
            OptionBuilder.hasArg().withArgName('query').withDescription('the query to execute').create('q'),
            OptionBuilder.hasArg().withArgName('printOpt').withDescription('the printOpt of the query').create('p'),
        ],
        requestBuilder: {String url ->
            if (cliOptions.id) {
                String id = cliOptions.id
                if (cliOptions.q) {
                    String query = cliOptions.q
                    String printOpt = cliOptions.p
                    String getUrl = "${url}/${id}/query?query=${query}"
                    if (printOpt) {
                        getUrl += "&printOpt=${printOpt}"
                    }
                    return new HttpGet(getUrl)
                }
                else {
                    throw new RuntimeException("The query option is not specified")
                }

            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        authenticator: DEFAULT_AUTHENTICATOR,
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return json.result.join("\n")
        }
    )

    /**
     * Consumes the DELETE /analyses/[analysis-id] response.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
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
                props.setProperty("jars", "${doc.g}:${doc.a}:${doc.latestVersion}")
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
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        login       : LOGIN,
        ping        : PING,
        list        : LIST,
        post        : POST,
        get         : GET,
        start       : START,
        stop        : STOP,
        post_process: POST_PROCESS,
        query       : QUERY,
        delete      : DELETE,
        mvnsearch   : SEARCH_MAVEN
    ]
}
