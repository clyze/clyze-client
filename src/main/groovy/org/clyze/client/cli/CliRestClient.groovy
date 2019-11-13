package org.clyze.client.cli

import groovy.transform.TypeChecked
import org.clyze.client.web.http.*
import org.clyze.client.web.api.*
import org.clyze.client.web.Helper as ClientHelper
import org.clyze.client.web.PostState
import org.apache.commons.cli.Option
import org.apache.http.HttpEntity

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
// @TypeChecked
class CliRestClient {

    private static final int DEFAULT_LIST_SIZE = 20

    private static final Option ID = Option.builder('id').hasArg().argName('id').
                                                   desc('the analysis id').build()

    private static final String getUserToken(boolean askForCredentialsIfEmpty, String host, int port) {
        String token = CliAuthenticator.getUserToken()
        if (!token && askForCredentialsIfEmpty) {
            //Ask for username and password
            COMMANDS.login.execute(host, port)
            token = CliAuthenticator.getUserToken()
        }
        token
    }    

    private static final Closure<String> DEFAULT_SUCCES = { HttpEntity entity ->
        return "OK"
    }

    /*
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

        String head = """${index == null ? "" : "$index."} ${analysisData.id}"""
        String sep  = ""
        head.length().times { sep += "#" }        

        return """\
               ${sep}               
               ${head}               
               ${sep}                                             
               Org      : ${analysisData.orgName}
               Project  : ${analysisData.projectName}
               Version  : ${analysisData.projectVersion}               
               Family   : ${analysisData.family}
               Name     : ${analysisData.name}               
               Inputs   : ${analysisData.options.INPUTS}
               Libraries: ${analysisData.options.LIBRARIES}
               HeapDLs  : ${analysisData.options.HEAPDL}
               Status   : ${analysisData.state}""".stripIndent()
    }
    */

    private static final CliRestCommand PING = new CliRestCommand(        
        name               : 'ping',
        description        : 'pings the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : LowLevelAPI.Requests.&ping,
        onSuccess          : DEFAULT_SUCCES,                
    )    

    private static final CliRestCommand LOGIN = new CliRestCommand(        
        name               : 'login',
        description        : 'login to the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            Map<String, String> credentials = CliAuthenticator.askForCredentials()
            return LowLevelAPI.Requests.login(credentials.username, credentials.password, host, port)                        
        },
        onSuccess          : { HttpEntity entity ->
            String token = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "token")
            CliAuthenticator.setUserToken(token)
            return "Logged in, token updated."
        }
    )    

    private static final CliRestCommand LIST_BUNDLES = new CliRestCommand(
        name               : 'list_bundles',
        description        : 'list the bundles stored in the remote server',
        //TODO add pagination options
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            return LowLevelAPI.Bundles.listBundles(token, null, null, host, port) //TODO: Fix this
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            json as String
        }
    )

    private static final CliRestCommand POST_DOOP_BUNDLE = new CliRestCommand(
        name               : 'post_doop_bundle',
        description        : 'posts a new doop bundle to the remote server',        
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        optionsBuilder     : { String host, int port ->
            def json = ClientHelper.createCommandForOptionsDiscovery("BUNDLE", new DefaultHttpClientLifeCycle()).execute(host, port)
            return ClientHelper.convertJsonEncodedOptionsToCliOptions(json.options as List)
        },
        requestBuilder     : { String host, int port ->            
            PostState post = new PostState()

            //options have been discovered here
            supportedOptions.findAll { cliOptions.hasOption(it.longOpt) }.each {
                post.addInputFromCliOption(it, cliOptions)
            }            

            String token = getUserToken(true, host, port)
            return LowLevelAPI.Bundles.createDoopBundle(token, null, post.asMultipart(), host, port)
        },
        onSuccess          : { HttpEntity entity ->
            String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
            return id
        }
    )    

    /**
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        //PING, LOGIN, LIST_BUNDLES, POST_DOOP_BUNDLE, POST_DOOP, POST_CCLYZER, LIST, GET, START, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART        
        PING, LOGIN, LIST_BUNDLES, POST_DOOP_BUNDLE
    ].collectEntries {
        [(it.name):it]
    }
}
