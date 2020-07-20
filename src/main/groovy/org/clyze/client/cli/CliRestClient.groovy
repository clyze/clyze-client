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
 *     <li>list_projects     - list the available projects
 *     <li>create_project    - create a project
 *     <li>create_sample_project-create a project based on a sample
 *     <li>get_project       - get a project
 *     <li>delete_project    - delete a project
 *
 *     <li>list_builds       - list the available builds
 *     <li>get_build         - get a build
 *     <li>post_build        - create a new build
 *     <li>delete_build      - delete a build
 *     <li>list_samples      - list the available sample builds
 *     <li>post_sample_build - create a new build, based on a given sample
 *
 *     <li>list_configurations - list the available configurations
 *     <li>get_config        - get a configuration
 *     <li>delete_config     - delete a configuration
 *     <li>clone_config      - clone a configuration
 *     <li>rename_config     - rename a configuration
 *     <li>get_rules         - get configuration rules
 *     <li>delete_rules      - delete configuration rules
 *     <li>paste_rules       - paste configuration rules
 *     <li>export_config     - export a configuration
 *     <li>get_output        - get an analysis output
 *
 *     <li>login             - authenticate user
 *     <li>ping              - check connection with server
 *     <li>analyze           - run an analysis
 *     <li>runtime           - check the runtime status of an analysis
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
            String project = readProjectNameFromConsole()
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
                String project = readProjectNameFromConsole()
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

    private static final CliRestCommand CREATE_PROJECT = new CliRestCommand(
            name               : 'create_project',
            description        : 'create an empty project in the remote server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token    = getUserToken(true, host, port)
                String user     = getUserName(false, host, port)
                String project  = readProjectNameFromConsole()
                String platform = readPlatformFromConsole()
                return LowLevelAPI.Projects.createProject(token, user, project, platform, host, port)
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
                String project = readProjectNameFromConsole()
                return LowLevelAPI.Projects.getProject(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_PROJECT = new CliRestCommand(
            name               : 'delete_project',
            description        : 'delete project stored in the remote server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                return LowLevelAPI.Projects.deleteProject(token, user, project, host, port)
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
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
            return convertJsonEncodedOptionsToCliOptions(json)
        },
        requestBuilder     : { String host, int port ->            
            PostState post = new PostState()

            //options have been discovered here
            supportedOptions.findAll { cliOptions.hasOption(it.longOpt) }.each {
                post.addInputFromCliOption(it as Option, cliOptions as OptionAccessor)
            }            

            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = readProjectNameFromConsole()
            String profile = readBuildProfileFromConsole()
            return LowLevelAPI.Builds.createBuild(token, user, project, profile, post.asMultipart(), host, port)
        },
        onSuccess          : { HttpEntity entity ->
            String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
            return id
        }
    )

    private static final CliRestCommand DELETE_BUILD = new CliRestCommand(
            name               : 'delete_build',
            description        : 'delete build stored in the remote server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token   = getUserToken(true, host, port)
                String user    = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build   = readBuildNameFromConsole()
                return LowLevelAPI.Builds.deleteBuild(token, user, project, build, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand POST_SAMPLE_BUILD = new CliRestCommand(
            name               : 'post_sample_build',
            description        : 'posts a new build to the remote server, based on a sample',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.getConfiguration(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand CLONE_CONFIGURATION = new CliRestCommand(
            name               : 'clone_config',
            description        : 'clone a build configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.cloneConfiguration(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand RENAME_CONFIGURATION = new CliRestCommand(
            name               : 'rename_config',
            description        : 'rename a build configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                String newName = readConfigFromConsole('new-name.json')
                return LowLevelAPI.Builds.renameConfiguration(token, user, project, build, config, newName, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand PASTE_CONFIGURATION_RULES = new CliRestCommand(
            name               : 'paste_rules',
            description        : 'pastes rules from a build configuration to another configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                String fromConfig = readConfigFromConsole('config2.json')
                return LowLevelAPI.Builds.pasteConfigurationRules(token, user, project, build, config, fromConfig, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_CONFIGURATION = new CliRestCommand(
            name               : 'delete_config',
            description        : 'delete a build configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                return LowLevelAPI.Builds.deleteConfiguration(token, user, project, build, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_RULES = new CliRestCommand(
            name               : 'delete_rules',
            description        : 'delete configuration rules',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                String idsLine = System.console().readLine('Rule IDs, separated by comma: ')
                List<String> ids = idsLine.tokenize(',')
                return LowLevelAPI.Builds.deleteRules(token, user, project, build, config, ids, host, port)
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                String originType = System.console().readLine("[Optional] Origin type: ")
                if (originType == '')
                    originType = null
                String start = System.console().readLine("[Optional] Pagination/start: ")
                Integer _start = start == '' ? null : Integer.valueOf(start)
                String count = System.console().readLine("[Optional] Pagination/count: ")
                Integer _count = count == '' ? null : Integer.valueOf(count)
                return LowLevelAPI.Builds.getRules(token, user, project, build, config, originType, _start, _count, host, port)
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
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
                String project = readProjectNameFromConsole()
                String build = readBuildNameFromConsole()
                String config = readConfigFromConsole()
                String output = System.console().readLine("Output: ")
                return LowLevelAPI.Builds.getOutput(token, user, project, build, config, output, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                LowLevelAPI.Responses.asString(entity)
            }
    )

    private static String readProjectNameFromConsole() {
        final String DEFAULT_PROJECT = 'samples'
        String project = System.console().readLine("Project (default: '${DEFAULT_PROJECT})': ")
        return ('' == project) ? DEFAULT_PROJECT : project
    }

    private static String readBuildNameFromConsole() {
        return System.console().readLine("Build: ")
    }

    private static String readPlatformFromConsole() {
        final String DEFAULT_PLATFORM = 'ANDROID'
        String platform = System.console().readLine("Project (default: '${DEFAULT_PLATFORM})': ")
        return ('' == platform) ? DEFAULT_PLATFORM : platform
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

    private static String readConfigFromConsole(String defaultConfig = 'clyze.json') {
        String config = System.console().readLine("Configuration (default: '${defaultConfig})': ")
        return ('' == config) ? defaultConfig : config
    }

    private static List<Option> convertJsonEncodedOptionsToCliOptions(Object json) {
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

    /**
     * The map of available commands.
     */
    public static final Map<String, CliRestCommand> COMMANDS = [
        // Projects
        LIST_PROJECTS, CREATE_PROJECT, CREATE_SAMPLE_PROJECT, GET_PROJECT, DELETE_PROJECT,
        // Builds
        LIST_BUILDS, LIST_SAMPLES, POST_BUILD, POST_SAMPLE_BUILD, GET_BUILD, DELETE_BUILD,
        // Configurations
        LIST_CONFIGURATIONS, GET_CONFIGURATION, CLONE_CONFIGURATION, RENAME_CONFIGURATION, DELETE_CONFIGURATION, EXPORT_CONFIGURATION, GET_RULES, DELETE_RULES, PASTE_CONFIGURATION_RULES,
        // Misc.
        PING, LOGIN, ANALYZE, GET_OUTPUT, RUNTIME
        // POST_DOOP, POST_CCLYZER, LIST, GET, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART
    ].collectEntries {
        [(it.name):it]
    }
}
