package doop.web.client

import doop.core.AnalysisOption
import doop.core.Doop
import doop.core.Helper
import groovy.json.JsonSlurper
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
/**
 * A client for a remote doop server.
 *
 * The client can execute the following commands via the remote server:
 * <ul>
 *     <li>ping  - check connection with server.
 *     <li>list  - list the available analyses.
 *     <li>post  - create a new analysis.
 *     <li>get   - retrieves an analysis.
 *     <li>start - start an analysis.
 *     <li>stop  - stop an analysis.
 *     <li>query - query a complete analysis.
 *
 * </ul>
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 11/2/2015
 */
class RestClient {

    private static final Option ID = OptionBuilder.hasArg().withArgName('id').
                                                   withDescription('the analysis id').create('id')

    private static final String processAnalysisData(def analysisData) {

        String result = """Analysis ID: ${analysisData.id}
   Name: ${analysisData.name}
   Jars: ${analysisData.jars.join(",")}
   Status: ${analysisData.state}
"""
        return result
    }

    /**
     * Consumes the GET /analyses response, ignoring the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysesResource}
     */
    private static final RestCommand PING = new RestCommand(
        name: 'ping',
        description: "Pings the remote server"
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
            return json.list.collect{ processAnalysisData(it) }.join("\n")
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
                        withDescription('The name of the analysis').create('a'),
                OptionBuilder.withLongOpt('jar').hasArgs(Option.UNLIMITED_VALUES).withArgName('jar').
                        withDescription("The jar files to analyze").withValueSeparator(',' as char).create('j')
        ] + Helper.convertAnalysisOptionsToCliOptions(Doop.ANALYSIS_OPTIONS.findAll { it.webUI }),
        buildRequest: {String url, OptionAccessor cliOptions ->

            //Get the name of the analysis (short option: a)
            String name = cliOptions.a
            //Get the jars of the analysis (short option: j)
            List<String> jars = cliOptions.js

            if (name) {
                if (jars) {
                    //TODO: We currently send only the first jar, treating it as a local file
                    File jar = Helper.checkFileOrThrowException(jars[0], "The jar option is invalid: ${jars[0]}")
                    HttpPost post = new HttpPost(url)
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create().
                                                                            addPart("jars", new FileBody(jar)).
                                                                            addPart("name", new StringBody(name))

                    /*
                     Iterate through the other cliOptions and add them to the multipart body.
                     TODO: Deal with options that accept files
                     */
                    Doop.ANALYSIS_OPTIONS.findAll { it.webUI && it.isFile }.each { AnalysisOption option ->
                        String cliOptionName = option.name
                        def optionValue = cliOptions[(cliOptionName)]
                        if (optionValue) {
                            if (option.argName) { //Only true-ish values are of interest (false or null values are ignored)
                                //if the cl option has an arg, the value of this arg defines the value of the respective
                                // analysis option
                                builder.addPart(cliOptionName, new StringBody(optionValue as String))
                            }
                            else {
                                //the cl option has no arg and thus it is a boolean flag, toggling the default value of
                                // the respective analysis option
                                def value = !option.value
                                builder.addPart(cliOptionName, new StringBody(value as String))
                            }
                        }
                    }
                    HttpEntity entity = builder.build()
                    post.setEntity(entity)
                    return post
                }
                else {
                    throw new RuntimeException("The jar option is not specified")
                }
            }
            else {
                throw new RuntimeException("The name option is not specified")
            }
        },
        onSuccess: { HttpEntity entity ->
            def json = new JsonSlurper().parse(entity.getContent(), "UTF-8")
            return "Analysis posted. ID: ${json.id}"
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
            return processAnalysisData(json.analysis)
        }
    )

    /**
     * Consumes the GET /analyses/[analysis-id]?status=start response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand START = new RestCommand(
        name:'start',
        description: "Starts an analysis on the remote server",
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpGet("${url}/${id}?status=start")
            }
            else {
                throw new RuntimeException("The id option is not specified")
            }
        }
    )

    /**
     * Consumes the GET /analyses/[analysis-id]?status=stop response, printing the result.
     * {@see doop.web.restlet.App, doop.web.restlet.api.AnalysisResource}
     */
    private static final RestCommand STOP = new RestCommand(
        name: 'stop',
        description: "Stops an analysis running on the remote server",
        options:[ID],
        buildRequest: {String url, OptionAccessor cliOptions ->
            if (cliOptions.id) {
                String id = cliOptions.id
                return new HttpGet("${url}/${id}?status=stop")
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
     * The list of available commands.
     */
    public static final List<RestCommand> COMMANDS = [PING, LIST, POST, GET, START, STOP, QUERY]

}
