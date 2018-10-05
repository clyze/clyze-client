package org.clyze.client.cli

import org.clyze.client.web.http.*
import org.clyze.client.web.api.*

import groovy.json.JsonSlurper
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import org.apache.log4j.Logger
import org.clyze.analysis.AnalysisFamilies
import org.clyze.analysis.AnalysisOption
import org.clyze.doop.CommandLineAnalysisFactory
import org.clyze.doop.core.Doop
import org.clyze.utils.FileOps

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

    private static final String getUserToken(boolean askForCredentialsIfEmpty, host, port) {
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
            String token = LowLevelAPI.Responses.parseJsonAndGetAttr("token", entity)
            CliAuthenticator.setUserToken(token)
            return "Logged in, token updated."
        }
    )    

    private static final CliRestCommand LIST_BUNDLES = new CliRestCommand(
        name               : 'list_bundles',
        description        : 'list the bundles stored in the remote server',
        options            : [], //TODO add pagination options
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            return LowLevelAPI.Requests.listBundles(token, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            json as String
        }
    )

    /**
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        //PING, LOGIN, LIST_BUNDLES, POST_DOOP_BUNDLE, POST_DOOP, POST_CCLYZER, LIST, GET, START, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART        
        PING, LOGIN, LIST_BUNDLES
    ].collectEntries {
        [(it.name):it]
    }
}
