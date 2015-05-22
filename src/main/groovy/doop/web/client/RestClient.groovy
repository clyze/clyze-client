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
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.log4j.Logger

/**
 * A client for a remote doop server.
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
class RestClient {

    private static final Option ID = OptionBuilder.hasArg().withArgName('id').
                                                   withDescription('the analysis id').create('id')

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
    private static final RestCommand LOGIN = new RestCommand(
        name: 'login',
        description: 'login to the remote server',
        endPoint: 'authenticate',
        authenticationRequired: false,
        buildRequest: { String url, OptionAccessor cliOptions ->
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
    private static final RestCommand PING = new RestCommand(
        name: 'ping',
        description: "Pings the remote server",
        endPoint: "ping",
        authenticationRequired: false
    )

    /**
     * Consumes the GET /analyses response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysesResource}
     */
    private static final RestCommand LIST = new RestCommand(
        name: 'list',
        description: "Lists the analyses of the remote server",
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
    private static final RestCommand POST = new RestCommand(
        name: 'post',
        description: "Posts a new analysis to the remote server",
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
        buildRequest: {String url, OptionAccessor cli ->

            String name, id
            List<String> jars
            Map<String, AnalysisOption> options

            if (cli.p) {
                //load the analysis options from the property file
                String file = cli.p
                Properties props = Helper.loadProperties(file)

                Helper.checkMandatoryProps(props)

                //Get the name of the analysis
                name = props.getProperty("analysis")
                //Get the jars of the analysis
                jars = props.getProperty("jar").split(",").collect { String s-> s.trim() }
                //Get the optional id of the analysis
                id = props.getProperty("id")

                options = Doop.createOptionsFromProperties(props) {AnalysisOption option -> option.webUI }
            }
            else {

                Helper.checkMandatoryArgs(cli)

                //Get the name of the analysis
                name = cli.a ?: null
                //Get the jars of the analysis
                jars = cli.js ?: null
                //Get the optional id of the analysis
                id = cli.id ?: null

                options = Doop.createOptionsFromCLI(cli) { AnalysisOption option -> option.webUI }
            }

            //create the HttpPost
            HttpPost post = new HttpPost(url)
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
            doop.web.client.Helper.buildPostRequest(id, name, options, jars, builder)
            HttpEntity entity = builder.build()
            Logger.getRootLogger().debug entity
            post.setEntity(entity)
            return post
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return "Analysis posted: ${json.id}"
        }
    )

    /**
     * Consumes the GET /analyses/[analysis-id] response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand GET = new RestCommand(
        name: 'get',
        description: "Gets the analysis from the remote server",
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpGet("${url}/${id}")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return processAnalysisData(null, json.analysis)
        }
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]?status=start response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand START = new RestCommand(
        name:'start',
        description: "Starts an analysis on the remote server",
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}?status=start")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        }
    )

    /**
     * Consumes the PUT /analyses/[analysis-id]?status=stop response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand STOP = new RestCommand(
        name: 'stop',
        description: "Stops an analysis running on the remote server",
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpPut("${url}/${id}?status=stop")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        }
    )

    /**
     * Consumes the GET /analyses/[analysis-id]/query response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.QueryAnalysisResource}
     */
    private static final RestCommand QUERY = new RestCommand(
        name: 'query',
        description: "Queries an analysis that has completed on the remote server",
        options:[
                ID,
                OptionBuilder.hasArg().withArgName('query').withDescription('the query to execute').create('q')
        ],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                if (cliOptions.q) {
                    String query = cliOptions.q
                    return new HttpGet("${url}/${id}/query?query=${query}")
                }
                else {
                    throw new RuntimeException("The query option is not specified")
                }

            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return json.result.join("\n")
        }
    )

    /**
     * Consumes the DELETE /analyses/[analysis-id] response.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand DELETE = new RestCommand(
        name:'delete',
        description: 'Delete the analysis from the remote server',
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpDelete("${url}/${id}")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        }
    )

    /**
     * Experimental - Search Maven Central and create doop.properties file(s) for the selected projects.
     */
    private static final RestCommand SEARCH_MAVEN = new RestCommand(
        name:'mvnsearch',
        description: 'Search Maven Central and create doop.properties files(s) for the selected projects.',
        authenticationRequired: false,
        options:[
            OptionBuilder.hasArg().withArgName('free text').withDescription('the search text').create('text')
        ],
        buildRequest: {String url, OptionAccessor cliOptions ->
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
    public static final Map<String, RestCommand> COMMANDS = [
        login    : LOGIN,
        ping     : PING,
        list     : LIST,
        post     : POST,
        get      : GET,
        start    : START,
        stop     : STOP,
        query    : QUERY,
        delete   : DELETE,
        mvnsearch: SEARCH_MAVEN
    ]

}
