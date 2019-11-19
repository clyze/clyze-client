package org.clyze.client.cli

import groovy.transform.TypeChecked
import org.clyze.client.cli.CliAuthenticator.Selector
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
 *     <li>list_projects     - list the available projects.
 *     <li>list_bundles      - list the available bundles.
 *     <li>post_bundle       - create a new bundle.
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

    private static final String getUserInfo(boolean askForCredentialsIfEmpty, String host, int port, Selector selector) {
        String data = CliAuthenticator.getUserInfo(selector)
        if (!data && askForCredentialsIfEmpty) {
            //Ask for username and password
            COMMANDS.login.execute(host, port)
            data = CliAuthenticator.getUserInfo(selector)
        }
        data
    }

    private static final String getUserToken(boolean askForCredentialsIfEmpty, String host, int port) {
        getUserInfo(askForCredentialsIfEmpty, host, port, Selector.TOKEN)
    }

    private static final String getUserName(boolean askForCredentialsIfEmpty, String host, int port) {
        getUserInfo(askForCredentialsIfEmpty, host, port, Selector.USERNAME)
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

    private static String LOGIN_LAST_USERNAME = null
    private static final CliRestCommand LOGIN = new CliRestCommand(        
        name               : 'login',
        description        : 'login to the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            Map<String, String> credentials = CliAuthenticator.askForCredentials()
            LOGIN_LAST_USERNAME = credentials.username
            return LowLevelAPI.Requests.login(credentials.username, credentials.password, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            String token = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "token")
            CliAuthenticator.setUserInfo(LOGIN_LAST_USERNAME, token)
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
            String user  = getUserName(false, host, port)
            String project = System.console().readLine("Project: ")
            return LowLevelAPI.Bundles.listBundles(token, user, project, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            println "== Bundles =="
            for (def result : json.results) {
                def arts = result.appArtifacts.collect { it.name }.toString()
                println "* ${result.displayName} (${result.profile.id}): ${arts}"
            }
            println ""
            json as String
        }
    )

    private static final CliRestCommand LIST_PROJECTS = new CliRestCommand(
        name               : 'list_projects',
        description        : 'list the projects stored in the remote server',
        //TODO add pagination options
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            return LowLevelAPI.Projects.getProjects(token, user, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            json as String
        }
    )

    private static final CliRestCommand POST_BUNDLE = new CliRestCommand(
        name               : 'post_bundle',
        description        : 'posts a new bundle to the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        optionsBuilder     : { String host, int port ->
            def json = ClientHelper.createCommandForOptionsDiscovery("BUNDLE", new DefaultHttpClientLifeCycle()).execute(host, port)
            return ClientHelper.convertJsonEncodedOptionsToCliOptions(json)
        },
        requestBuilder     : { String host, int port ->            
            PostState post = new PostState()

            //options have been discovered here
            supportedOptions.findAll { cliOptions.hasOption(it.longOpt) }.each {
                post.addInputFromCliOption(it, cliOptions)
            }            

            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = System.console().readLine("Project: ")
            String DEFAULT_PROFILE = 'apiTargetAndroid25'
            String profile = System.console().readLine("Profile (default is '${DEFAULT_PROFILE}'): ")
            if ((profile == null) || (profile == "")) {
                profile = DEFAULT_PROFILE
            }
            return LowLevelAPI.Bundles.createBundle(token, user, project, profile, post.asMultipart(), host, port)
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
        //PING, LOGIN, LIST_BUNDLES, POST_BUNDLE, POST_DOOP, POST_CCLYZER, LIST, GET, START, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART
        PING, LOGIN, LIST_PROJECTS, LIST_BUNDLES, POST_BUNDLE
    ].collectEntries {
        [(it.name):it]
    }
}
