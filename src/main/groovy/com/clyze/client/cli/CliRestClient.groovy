package com.clyze.client.cli

import com.clyze.client.web.PostState
import com.clyze.client.web.SnapshotInput
import com.clyze.client.web.api.LowLevelAPI
import com.clyze.client.web.api.Remote
import com.clyze.client.web.http.DefaultHttpClientLifeCycle
// import groovy.transform.TypeChecked
import com.clyze.client.cli.CliAuthenticator.Selector
import groovy.cli.commons.OptionAccessor
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
 *     <li>get_project_options - show the options of a project
 *
 *     <li>list_snapshots    - list the available snapshots
 *     <li>get_snapshot      - get a snapshot
 *     <li>post_snapshot     - create a new snapshot
 *     <li>delete_snapshot   - delete a snapshot
 *     <li>list_samples      - list the available sample snapshots
 *     <li>post_sample       - create a new snapshot, based on a given sample
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
 *     <li>repackage         - run automated repackaging
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
    //Used by comands to list analyses and snapshots
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

    private static final CliRestCommand LIST_SNAPSHOTS = new CliRestCommand(
        name               : 'list_snapshots',
        description        : 'list the snapshots stored in server',
        //TODO add pagination options
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.listSnapshots(token, user, project, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            def json = LowLevelAPI.Responses.parseJson(entity)
            println "== Snapshots =="
            for (def result : json.results) {
                def arts = result.artifacts.collect { it.name }.toString()
                println "* ${result.displayName}: ${arts}"
            }
            println ""
            json as String
        }
    )

    private static final CliRestCommand LIST_SAMPLES = new CliRestCommand(
            name               : 'list_samples',
            description        : 'list the sample snapshots available in server',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                return LowLevelAPI.Snapshots.listSamples(token, user, project, host, port)
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
        description        : 'list the projects',
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
            description        : 'create an empty project',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token    = getUserToken(true, host, port)
                String user     = getUserName(false, host, port)
                String project  = readProjectNameFromConsole(cliOptions)
                String[] stacks = readStacksFromConsole(cliOptions)
                return LowLevelAPI.Projects.createProject(token, user, project, stacks, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_PROJECT_OPTIONS = new CliRestCommand(
            name               : 'get_project_options',
            description        : 'get options of project',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                return LowLevelAPI.Projects.getProjectOptions(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_PROJECT = new CliRestCommand(
            name               : 'get_project',
            description        : 'get project',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                return LowLevelAPI.Projects.getProject(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_PROJECT = new CliRestCommand(
            name               : 'delete_project',
            description        : 'delete project',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                return LowLevelAPI.Projects.deleteProject(token, user, project, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_SNAPSHOT = new CliRestCommand(
            name               : 'get_snapshot',
            description        : 'read a snapshot',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                return LowLevelAPI.Snapshots.getSnapshot(token, user, project, snapshot, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand POST_SNAPSHOT = new CliRestCommand(
        name               : 'post_snapshot',
        description        : 'posts a new snapshot to the remote server',
        httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
        requestBuilder     : { String host, int port ->
            String token = getUserToken(true, host, port)
            String user  = getUserName(false, host, port)
            String project = readProjectNameFromConsole(cliOptions)
            printProjectOptions(host, port, user, project)
            PostState postState = getPostState(cliOptions)
            return LowLevelAPI.Snapshots.createSnapshot(token, user, project, postState, host, port)
        },
        onSuccess          : { HttpEntity entity ->
            String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
            return id
        }
    )

    private static final PostState getPostState(OptionAccessor cliOptions) {
        PostState postState = new PostState()
        readSnapshotInputsFromConsole(cliOptions).forEach { String k, SnapshotInput input ->
            postState.addInput(k, input)
        }
        return postState
    }

    private static final void printProjectOptions(String host, int port, String user, String project) {
        Map<String, Object> options = Remote.at(host, port).getProjectOptions(user, project)
        options.forEach{k, v ->
            println ("Available options (${k}): " + v.collect {it.id + (it.isFile ? "@path" : "=value")})
        }
    }

    private static final CliRestCommand REPACKAGE = new CliRestCommand(
            name               : 'repackage',
            description        : 'automated repackaging endpoint',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                PostState postState = getPostState(cliOptions)
                return LowLevelAPI.Projects.repackageSnapshotForCI(token, user, project, postState, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
                return id
            }
    )

    private static final CliRestCommand DELETE_SNAPSHOT = new CliRestCommand(
            name               : 'delete_snapshot',
            description        : 'delete snapshot',
            //TODO add pagination options
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token   = getUserToken(true, host, port)
                String user    = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot   = readSnapshotNameFromConsole(cliOptions)
                return LowLevelAPI.Snapshots.deleteSnapshot(token, user, project, snapshot, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand POST_SAMPLE_SNAPSHOT = new CliRestCommand(
            name               : 'post_sample_snapshot',
            description        : 'posts a new snapshot to the remote server, based on a sample',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                final String DEFAULT_SAMPLE_NAME = 'apps-android-wikipedia'
                String sampleName = System.console().readLine("Sample name (default: '${DEFAULT_SAMPLE_NAME}'): ")
                if ('' == sampleName)
                    sampleName = DEFAULT_SAMPLE_NAME
                return LowLevelAPI.Snapshots.createSnapshotFromSample(token, user, project, sampleName, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                String id = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
                return id
            }
    )

    private static final CliRestCommand CREATE_SAMPLE_PROJECT = new CliRestCommand(
            name               : 'create_sample_project',
            description        : 'creates a new project based on a sample',
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
            description        : 'list the configurations of a snapshot',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                return LowLevelAPI.Snapshots.listConfigurations(token, user, project, snapshot, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_CONFIGURATION = new CliRestCommand(
            name               : 'get_config',
            description        : 'get a snapshot configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.getConfiguration(token, user, project, snapshot, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand CLONE_CONFIGURATION = new CliRestCommand(
            name               : 'clone_config',
            description        : 'clone a snapshot configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.cloneConfiguration(token, user, project, snapshot, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand RENAME_CONFIGURATION = new CliRestCommand(
            name               : 'rename_config',
            description        : 'rename a snapshot configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String newName = readConfigFromConsole('new-name.json')
                return LowLevelAPI.Snapshots.renameConfiguration(token, user, project, snapshot, config, newName, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand PASTE_CONFIGURATION_RULES = new CliRestCommand(
            name               : 'paste_rules',
            description        : 'pastes rules from a snapshot configuration to another configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String fromConfig = readConfigFromConsole('config2.json')
                return LowLevelAPI.Snapshots.pasteConfigurationRules(token, user, project, snapshot, config, fromConfig, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_CONFIGURATION = new CliRestCommand(
            name               : 'delete_config',
            description        : 'delete a snapshot configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.deleteConfiguration(token, user, project, snapshot, config, host, port)
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
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String idsLine = System.console().readLine('Rule IDs, separated by comma: ')
                List<String> ids = idsLine.tokenize(',')
                return LowLevelAPI.Snapshots.deleteRules(token, user, project, snapshot, config, ids, host, port)
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
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String originType = System.console().readLine("[Optional] Origin type: ")
                if (originType == '')
                    originType = null
                String start = System.console().readLine("[Optional] Pagination/start: ")
                Integer _start = start == '' ? null : Integer.valueOf(start)
                String count = System.console().readLine("[Optional] Pagination/count: ")
                Integer _count = count == '' ? null : Integer.valueOf(count)
                return LowLevelAPI.Snapshots.getRules(token, user, project, snapshot, config, originType, _start, _count, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand POST_RULE = new CliRestCommand(
            name               : 'post_rule',
            description        : 'post configuration rule',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String ruleBody = System.console().readLine("Rule body: ")
                return LowLevelAPI.Snapshots.postRule(token, user, project, snapshot, config, ruleBody, null, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand PUT_RULE = new CliRestCommand(
            name               : 'put_rule',
            description        : 'put configuration rule (e.g. edit comment)',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String ruleId = System.console().readLine("Rule id: ")
                String ruleBody = System.console().readLine("Rule body: ")
                String comment = System.console().readLine("Rule comment: ")
                return LowLevelAPI.Snapshots.putRule(token, user, project, snapshot, config, ruleId, ruleBody, comment, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand DELETE_RULE = new CliRestCommand(
            name               : 'delete_rule',
            description        : 'delete configuration rule',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String ruleId = System.console().readLine("Rule id: ")
                return LowLevelAPI.Snapshots.deleteRule(token, user, project, snapshot, config, ruleId, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand EXPORT_CONFIGURATION = new CliRestCommand(
            name               : 'export_config',
            description        : 'export a snapshot configuration',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.exportConfiguration(token, user, project, snapshot, config, host, port)
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
            description        : 'run an analysis on a code snapshot',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.analyze(token, user, project, snapshot, config, host, port)
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
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                return LowLevelAPI.Snapshots.getRuntime(token, user, project, snapshot, config, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                def json = LowLevelAPI.Responses.parseJson(entity)
                json as String
            }
    )

    private static final CliRestCommand GET_OUTPUT = new CliRestCommand(
            name               : 'get_output',
            description        : 'get a snapshot output',
            httpClientLifeCycle: new DefaultHttpClientLifeCycle(),
            requestBuilder     : { String host, int port ->
                String token = getUserToken(true, host, port)
                String user  = getUserName(false, host, port)
                String project = readProjectNameFromConsole(cliOptions)
                String snapshot = readSnapshotNameFromConsole(cliOptions)
                String config = readConfigFromConsole()
                String output = System.console().readLine("Output: ")
                return LowLevelAPI.Snapshots.getOutput(token, user, project, snapshot, config, output, host, port)
            },
            onSuccess          : { HttpEntity entity ->
                LowLevelAPI.Responses.asString(entity)
            }
    )

    private static String readProjectNameFromConsole(OptionAccessor cliOptions) {
        final String DEFAULT_PROJECT = 'test-project'
        String project = cliOptions['project'] as String
        if (project)
            println "Assuming project = ${project}"
        else
            project = System.console().readLine("Project (default: '${DEFAULT_PROJECT})': ")
        return ('' == project) ? DEFAULT_PROJECT : project
    }

    private static Map<String, SnapshotInput> readSnapshotInputsFromConsole(OptionAccessor cliOptions) {
        Map<String, SnapshotInput> inputs = new HashMap<>()
        Collection<String> tokens = (cliOptions['inputs'] as Collection<String>) ?: null
        if (tokens)
            println "Assuming inputs = ${tokens}"
        else
            tokens = System.console().readLine("Inputs (separated by spaces, example: 'app@path.jar jvm_platform=java_8'): ").tokenize(' ') as Collection<String>
        for (String token : tokens) {
            int atIdx = token.indexOf('@')
            if (atIdx > 0)
                inputs.put(token.substring(0, atIdx), new SnapshotInput(true, token.substring(atIdx + 1)))
            else {
                int eqIdx = token.indexOf('=')
                if (eqIdx > 0)
                    inputs.put(token.substring(0, eqIdx), new SnapshotInput(false, token.substring(eqIdx + 1)))
                else
                    throw new RuntimeException('ERROR: Bad snapshot input: ' + token)
            }
        }
        return inputs
    }

    private static String[] readStacksFromConsole(OptionAccessor cliOptions) {
        final String DEFAULT_STACK = 'jvm'
        Collection<String> stacks = cliOptions['stacks'] as Collection<String>
        if (stacks)
            println "Assuming stacks = ${stacks}"
        else
            stacks = System.console().readLine("Project stacks (separated by spaces, default: '${DEFAULT_STACK})': ").trim().tokenize(' ')
        return stacks ?: new String[] {DEFAULT_STACK}
    }

    private static String readSnapshotNameFromConsole(OptionAccessor cliOptions) {
        final String DEFAULT_SNAPSHOT = 'snapshot1'
        String snapshot = cliOptions['snapshot']
        if (snapshot)
            println "Assuming snapshot = ${snapshot}"
        else
            snapshot = System.console().readLine("Snapshot (default: '${DEFAULT_SNAPSHOT}'): ")
        return ('' == snapshot) ? DEFAULT_SNAPSHOT : snapshot
    }

    private static String readConfigFromConsole(String defaultConfig = 'clyze.json') {
        String config = System.console().readLine("Configuration (default: '${defaultConfig})': ")
        return ('' == config) ? defaultConfig : config
    }

    private static Set<Option> convertJsonEncodedOptionsToCliOptions(Object json) {
        if (!json?.results)
            return new HashSet<>()
        Set<Option> ret = new HashSet<>()
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
            LIST_PROJECTS, CREATE_PROJECT, CREATE_SAMPLE_PROJECT, GET_PROJECT, DELETE_PROJECT, GET_PROJECT_OPTIONS,
            // Snapshots
            LIST_SNAPSHOTS, LIST_SAMPLES, POST_SNAPSHOT, POST_SAMPLE_SNAPSHOT, GET_SNAPSHOT, DELETE_SNAPSHOT,
            // Configurations
            LIST_CONFIGURATIONS, GET_CONFIGURATION, CLONE_CONFIGURATION, RENAME_CONFIGURATION, DELETE_CONFIGURATION, EXPORT_CONFIGURATION, GET_RULES, POST_RULE, DELETE_RULES, PUT_RULE, DELETE_RULE, PASTE_CONFIGURATION_RULES,
            // Misc.
            PING, LOGIN, REPACKAGE, ANALYZE, GET_OUTPUT, RUNTIME
            // POST_DOOP, POST_CCLYZER, LIST, GET, STOP, POST_PROCESS, RESET, RESTART, DELETE, SEARCH_MAVEN, QUICKSTART
    ].collectEntries {
        [(it.name):it]
    }
}
