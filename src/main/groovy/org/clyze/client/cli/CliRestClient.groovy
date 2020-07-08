package org.clyze.client.cli

import groovy.cli.commons.OptionAccessor
// import groovy.transform.TypeChecked
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
 *     <li>login             - authenticate user
 *     <li>ping              - check connection with server
 *     <li>list_projects     - list the available projects
 *     <li>get_project       - get a project
 *     <li>create_sample_project-create a project based on a sample
 *     <li>analyze           - run an analysis
 *     <li>runtime           - check the runtime status of an analysis
 *     <li>get_config        - get a configuration
 *     <li>get_rules         - get configuration rules
 *     <li>export_config     - export a configuration
 *     <li>get_output        - get an analysis output
 *     <li>list_configurations - list the available configurations
 *     <li>list_builds       - list the available builds
 *     <li>get_build         - get a build
 *     <li>post_build        - create a new build
 *     <li>list_samples      - list the available sample builds
 *     <li>post_sample       - create a new build, based on a given sample
 *     <li>list              - list the available analyses
 *     <li>post_doop         - create a new doop analysis
 *     <li>post_cclyzer      - create a new cclyzer analysis
 *     <li>get               - retrieves an analysis
 *     <li>stop              - stop an analysis
 *     <li>query             - query a complete analysis
 *     <li>delete            - delete an analysis
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
    //Used by comands to list analyses and builds
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

    private static final CliRestCommand LIST_BUILDS = new CliRestCommand(
        name               : 'list_builds',
        description        : 'list the builds stored in the remote server',
        //TODO add pagination options
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = readProjectFromConsole()
            return LowLevelAPI.Builds.listBuilds(token, user, project, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            println "== Builds =="
            for (def result : json.results) {
                def arts = result.appArtifacts.collect { it.name }.toString()
                println "* ${result.displayName} (${result.profile.id}): ${arts}"
            }
            println ""
            json as String
        }
    )

    private static final CliRestCommand LIST_SAMPLES = new CliRestCommand(
            name               : 'list_samples',
            description        : 'list the sample builds available in the remote server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                return LowLevelAPI.Builds.listSamples(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                println "== Samples =="
                for (def result : json.samples) {
                    println "* ${result}"
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

    private static final CliRestCommand GET_PROJECT = new CliRestCommand(
            name               : 'get_project',
            description        : 'get project stored in the remote server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                return LowLevelAPI.Projects.getProject(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_BUILD = new CliRestCommand(
            name               : 'get_build',
            description        : 'get a build',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                return LowLevelAPI.Builds.getBuild(token, user, project, build, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand POST_BUILD = new CliRestCommand(
        name               : 'post_build',
        description        : 'posts a new build to the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        optionsBuilder     : { String host, int port ->
            def json = ClientHelper.createCommandForOptionsDiscovery("BUILD", new DefaultHttpClientLifeCycle()).execute(host, port)
            return ClientHelper.convertJsonEncodedOptionsToCliOptions(json)
        },
        requestBuilder     : { String host, int port ->            
            PostState post = new PostState()

            //options have been discovered here
            supportedOptions.findAll { cliOptions.hasOption(it.longOpt) }.each {
                post.addInputFromCliOption(it as Option, cliOptions as OptionAccessor)
            }            

            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = readProjectFromConsole()
            String profile = readBuildProfileFromConsole()
            return LowLevelAPI.Builds.createBuild(token, user, project, profile, post.asMultipart(), host, port)
        },
        onSuccess          : { HttpEntity entity ->
            String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
            return id
        }
    )

    private static final CliRestCommand POST_SAMPLE = new CliRestCommand(
            name               : 'post_sample',
            description        : 'posts a new build to the remote server, based on a sample',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                final String DEFAULT_SAMPLE_NAME = 'apps-android-wikipedia'
                String sampleName = System.console().readLine("Sample name (default: '${DEFAULT_SAMPLE_NAME}'): ")
                if ('' == sampleName)
                    sampleName = DEFAULT_SAMPLE_NAME
                return LowLevelAPI.Builds.createBuildFromSample(token, user, project, sampleName, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
                return id
            }
    )

    private static final CliRestCommand CREATE_SAMPLE_PROJECT = new CliRestCommand(
            name               : 'create_sample_project',
            description        : 'creates a new project in the remote server, based on a sample',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                return LowLevelAPI.Projects.createSampleProject(token, user, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand LIST_CONFIGURATIONS = new CliRestCommand(
            name               : 'list_configurations',
            description        : 'list the configurations of a build',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                return LowLevelAPI.Builds.listConfigurations(token, user, project, build, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_CONFIGURATION = new CliRestCommand(
            name               : 'get_config',
            description        : 'get a build configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.getConfiguration(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_RULES = new CliRestCommand(
            name               : 'get_rules',
            description        : 'get configuration rules',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                String origin = System.console().readLine("[Optional] Origin: ")
                return LowLevelAPI.Builds.getRules(token, user, project, build, config, origin, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand EXPORT_CONFIGURATION = new CliRestCommand(
            name               : 'export_config',
            description        : 'export a build configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.exportConfiguration(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                String conf = LowLevelAPI.Responses.asString(entity)
                File f = File.createTempFile("exported-configuration", null)
                f.text = conf
                println "Configuration written to ${f}"
                conf
            }
    )

    private static final CliRestCommand ANALYZE = new CliRestCommand(
            name               : 'analyze',
            description        : 'run an analysis on a code build',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                String profile = readAnalysisProfileFromConsole()
                return LowLevelAPI.Builds.analyze(token, user, project, build, config, profile, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand RUNTIME = new CliRestCommand(
            name               : 'runtime',
            description        : 'show runtime stats for an analysis',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.getRuntime(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_OUTPUT = new CliRestCommand(
            name               : 'get_output',
            description        : 'get a build output',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectFromConsole()
                String build = System.console().readLine("Build: ")
                String config = readConfigFromConsole()
                String output = System.console().readLine("Output: ")
                return LowLevelAPI.Builds.getOutput(token, user, project, build, config, output, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                LowLevelAPI.Responses.asString(entity)
            }
    )

    private static String readProjectFromConsole() {
        final String DEFAULT_PROJECT = 'samples'
        String project = System.console().readLine("Project (default: '${DEFAULT_PROJECT})': ")
        return ('' == project) ? DEFAULT_PROJECT : project
    }

    private static String readBuildProfileFromConsole() {
        final String DEFAULT_PROFILE = 'proAndroid'
        String profile = System.console().readLine("Profile (default is '${DEFAULT_PROFILE}'): ")
        return ((profile == null) || (profile == "")) ? DEFAULT_PROFILE : profile
    }

    private static String readAnalysisProfileFromConsole() {
        final String DEFAULT_ANALYSIS_PROFILE = 'r8'
        String profile = System.console().readLine("Profile (default is '${DEFAULT_ANALYSIS_PROFILE}'): ")
        return ((profile == null) || (profile == "")) ? DEFAULT_ANALYSIS_PROFILE : profile
    }

    private static String readConfigFromConsole() {
        final String DEFAULT_CONFIG = 'clyze.json'
        String config = System.console().readLine("Configuration (default: '${DEFAULT_CONFIG})': ")
        return ('' == config) ? DEFAULT_CONFIG : config
    }

    /**
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        // POST_DOOP, POST_CCLYZER, LIST, GET, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART
        PING, LOGIN, LIST_PROJECTS, LIST_BUILDS, LIST_SAMPLES, GET_PROJECT, GET_BUILD, POST_BUILD, ANALYZE,
        POST_SAMPLE, EXPORT_CONFIGURATION, GET_CONFIGURATION, GET_OUTPUT, GET_RULES, LIST_CONFIGURATIONS, RUNTIME,
        CREATE_SAMPLE_PROJECT
    ].collectEntries {
        [(it.name):it]
    }
}
