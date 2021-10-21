package com.clyze.client.cli

import com.clyze.client.web.PostState
import com.clyze.client.web.SnapshotInput
import com.clyze.client.web.AuthToken
import com.clyze.client.web.api.LowLevelAPI
import com.clyze.client.web.api.Remote
import com.clyze.client.web.http.DefaultHttpClientLifeCycle
import com.clyze.client.web.http.HttpStringClientCommand
import groovy.cli.commons.OptionAccessor
import groovy.transform.CompileStatic
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpUriRequest
import org.clyze.persistent.metadata.JSONUtil

/**
 * A CLI Rest Client command.
 */
@CompileStatic
abstract class CliRestCommand extends HttpStringClientCommand {

    /** The name of the command */
    final String name

    /** The description of the command */
    final String description

    /** The command line options the command has been actually invoked with */
    OptionAccessor cliOptions

    protected CliRestCommand(String name, String description) {
        super(new DefaultHttpClientLifeCycle())
        this.name = name
        this.description = description
    }

    @Override
    String onSuccess(HttpEntity entity) {
        // Convert string to map and then back to pretty string.
        return prettyPrintMap(JSONUtil.toMap(entity.content.text))
    }

    static String prettyPrintMap(Map<?, ?> map) {
        return JSONUtil.objectWriter.writeValueAsString(map)
    }

    static final CliRestCommand PING = new CliRestCommand('ping', 'pings the server') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            return LowLevelAPI.Requests.ping(hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return 'OK'
        }
    }

    static final CliRestCommand DIAGNOSE = new CliRestCommand('diagnose', 'calls the server "diagnose" endpoint') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            return LowLevelAPI.Requests.diagnose(hostPrefix)
        }
    }

    private static String LOGIN_LAST_USERNAME = null
    static final Tuple2<String, AuthToken> readLogin(OptionAccessor cliOptions) {
        String user = readUserFromConsole(cliOptions)
        String tokenValue = readTokenFromConsole(cliOptions)
        LOGIN_LAST_USERNAME = user
        return new Tuple2<>(user, new AuthToken(user, tokenValue))
    }


    static final CliRestCommand CLEAN_DEPLOY = new CliRestCommand('clean_deploy', 'cleans the server for deployment') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            return LowLevelAPI.Requests.cleanDeploy(hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return 'OK'
        }
    }

    static final CliRestCommand LOGIN = new CliRestCommand('login', 'login to the server') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            Tuple2<String, AuthToken> login = readLogin(cliOptions)
            String user = login.v1
            AuthToken token = login.v2
            return LowLevelAPI.Requests.login(user, token, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            String token = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "token")
            CliAuthenticator.setUserInfo(LOGIN_LAST_USERNAME, token)
            return "Logged in, token updated."
        }
    }

    static final CliRestCommand LIST_USERS = new CliRestCommand('list_users', 'list the users') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            return LowLevelAPI.Requests.listUsers(token, user, hostPrefix)
        }
    }

    static final CliRestCommand LIST_SNAPSHOTS = new CliRestCommand('list_snapshots',  'list the snapshots stored in server') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.listSnapshots(token, user, project, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            def json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
            println "== Snapshots =="
            for (def result : json.get('results') as Collection<Map<String, Object>>) {
                def arts = (result.get('artifacts') as Collection<Map<String, Object>>)
                        ?.collect { it?.get('name') }
                println "* ${result.displayName}: ${arts}"
            }
            println ""
            json as String
        }
    }

    static final CliRestCommand LIST_PROJECTS = new CliRestCommand('list_projects', 'list the projects') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            return LowLevelAPI.Projects.getProjects(token, user, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            def json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
            println "== Projects =="
            for (Map<String, Object> result : json.get('results') as Collection<Map<String, Object>>)
                println "* ${result.get('name')} (id: ${result.get('id')})"
            println ""
            return prettyPrintMap(json)
        }
    }

    static final CliRestCommand LIST_PUBLIC_PROJECTS = new CliRestCommand('list_public_projects', 'list the public projects') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserToken(null, cliOptions, true, hostPrefix)
            return LowLevelAPI.Projects.getPublicProjects(token, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            def json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
            println "== Projects =="
            for (Map<String, Object> result : json.get('results') as Collection<Map<String, Object>>)
                println "* ${result.get('name')} (id: ${result.get('id')})"
            println ""
            return prettyPrintMap(json)
        }
    }

    static final CliRestCommand LIST_STACKS = new CliRestCommand('list_stacks', 'list the available stacks') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            return LowLevelAPI.Requests.listStacks(hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            def json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
            println "== Stacks =="
            for (def result : json.get('results') as Collection<Map<String, Object>>)
                println "* ${result}"
            println ""
            return prettyPrintMap(json)
        }
    }

    static final CliRestCommand CREATE_PROJECT = new CliRestCommand('create_project', 'create an empty project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            List<String> stacks = readStacksFromConsole(cliOptions)
            String isPublic = readOptionFromConsole(cliOptions, 'public', 'Project is public (true/false)', 'false')
            return LowLevelAPI.Projects.createProject(token, user, project, stacks, isPublic, hostPrefix)
        }
    }

    static final CliRestCommand GET_PROJECT_INPUTS = new CliRestCommand('get_project_inputs', 'get input options of project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProjectInputs(token, user, project, hostPrefix)
        }
    }

    static final CliRestCommand GET_PROJECT_ANALYSES = new CliRestCommand('get_project_analyses', 'get the analyses supported by a project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProjectAnalyses(token, user, project, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            def json = LowLevelAPI.Responses.parseJson(entity) as Map<String, Object>
            println "== Analyses =="
            for (def result : json.get('results') as Collection<Map<String, Object>>)
                println "* ${result.get('id')} ('${result.get('displayName')}')"
            println ""
            return prettyPrintMap(json)
        }
    }

    static final CliRestCommand GET_PROJECT = new CliRestCommand('get_project', 'get project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProject(token, user, project, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_PROJECT = new CliRestCommand('delete_project', 'delete project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.deleteProject(token, user, project, hostPrefix)
        }
    }

    static final CliRestCommand GET_SNAPSHOT = new CliRestCommand('get_snapshot', 'read a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getSnapshot(token, user, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand GET_SYMBOL = new CliRestCommand('get_symbol', 'read a symbol from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String symbol = readSymbolFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getSymbol(token, user, project, snapshot, symbol, hostPrefix)
        }
    }

    static final CliRestCommand GET_SYMBOLS = new CliRestCommand('get_symbols', 'get the symbols of a given code line') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String line = readLineFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Requests.getSymbols(token, user, project, snapshot, config, file, line, hostPrefix)
        }
    }

    static final CliRestCommand GET_FILES = new CliRestCommand('get_files', 'read the snapshot files') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String artifact = readArtifactFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getFiles(token, user, project, snapshot, artifact, hostPrefix)
        }
    }

    static final CliRestCommand GET_FILE = new CliRestCommand('get_file', 'read a snapshot file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String artifact = readArtifactFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getFile(token, user, project, snapshot, artifact, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_CODE_HINTS = new CliRestCommand('get_code_hints', 'get the hints for a snapshot code file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getCodeFileHints(token, user, project, snapshot, config, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_CODE_FILE = new CliRestCommand('get_code_file', 'read a snapshot code file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getCodeFile(token, user, project, snapshot, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_OUTPUT_FILE = new CliRestCommand('get_output_file', 'read an analysis output file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysis = readAnalysisIdFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysisOutputFile(token, user, project, snapshot, config, analysis, file, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            // Output files can be anything, just return their contents.
            return entity.content.text
        }
    }

    static final CliRestCommand POST_SNAPSHOT = new CliRestCommand('post_snapshot', 'posts a new snapshot to the server') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            printProjectOptions(hostPrefix, user, token, project)
            PostState postState = getPostState(cliOptions)
            return LowLevelAPI.Snapshots.createSnapshot(token, user, project, postState, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
        }
    }

    static final CliRestCommand REPACKAGE = new CliRestCommand('repackage', 'automated repackaging endpoint') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            PostState postState = getPostState(cliOptions)
            return LowLevelAPI.Projects.repackageSnapshotForCI(token, user, project, postState, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
        }
    }

    static final CliRestCommand DELETE_SNAPSHOT = new CliRestCommand('delete_snapshot', 'delete snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteSnapshot(token, user, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand LIST_CONFIGURATIONS = new CliRestCommand('list_configurations', 'list the configurations of a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.listConfigurations(token, user, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand GET_ANALYSIS = new CliRestCommand('get_analysis', 'reads an analysis from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysis(token, user, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand GET_ANALYSIS_RUNTIME = new CliRestCommand('get_analysis_runtime', 'read analysis runtime information') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysisRuntime(token, user, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_ANALYSIS = new CliRestCommand('delete_analysis', 'deletes an analysis from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteAnalysis(token, user, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand EXECUTE_ANALYSIS_ACTION = new CliRestCommand('execute_action', 'executes an analysis action (such as "stop" or "restart")') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            String action = readActionFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.executeAnalysisAction(token, user, project, snapshot, config, action, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand GET_CONFIGURATION = new CliRestCommand('get_config', 'get a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getConfiguration(token, user, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand CLONE_CONFIGURATION = new CliRestCommand('clone_config', 'clone a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.cloneConfiguration(token, user, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand RENAME_CONFIGURATION = new CliRestCommand('rename_config', 'rename a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String newName = readConfigFromConsole(null, 'new-name.json')
            return LowLevelAPI.Snapshots.renameConfiguration(token, user, project, snapshot, config, newName, hostPrefix)
        }
    }

    static final CliRestCommand PASTE_CONFIGURATION_RULES = new CliRestCommand('paste_rules', 'pastes rules from a snapshot configuration to another configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String fromConfig = readConfigFromConsole(null, 'config2.json')
            return LowLevelAPI.Snapshots.pasteConfigurationRules(token, user, project, snapshot, config, fromConfig, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_CONFIGURATION = new CliRestCommand('delete_config', 'delete a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteConfiguration(token, user, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_RULES = new CliRestCommand('delete_rules', 'delete configuration rules') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String idsLine = System.console().readLine('Rule IDs, separated by comma: ')
            List<String> ids = idsLine.tokenize(',')
            return LowLevelAPI.Snapshots.deleteRules(token, user, project, snapshot, config, ids, hostPrefix)
        }
    }

    static final CliRestCommand GET_RULES = new CliRestCommand('get_rules', 'get configuration rules') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String originType = System.console().readLine("[Optional] Origin type: ")
            if (originType == '')
                originType = null
            String start = System.console().readLine("[Optional] Pagination/start: ")
            Integer _start = start == '' ? null : Integer.valueOf(start)
            String count = System.console().readLine("[Optional] Pagination/count: ")
            Integer _count = count == '' ? null : Integer.valueOf(count)
            return LowLevelAPI.Snapshots.getRules(token, user, project, snapshot, config, originType, _start, _count, hostPrefix)
        }
    }

    static final CliRestCommand POST_RULE = new CliRestCommand('post_rule', 'post configuration rule') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleBody = System.console().readLine("Rule body: ")
            return LowLevelAPI.Snapshots.postRule(token, user, project, snapshot, config, ruleBody, null, hostPrefix)
        }
    }

    static final CliRestCommand PUT_RULE = new CliRestCommand('put_rule', 'put configuration rule (e.g. edit comment)') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleId = System.console().readLine("Rule id: ")
            String ruleBody = System.console().readLine("Rule body: ")
            String comment = System.console().readLine("Rule comment: ")
            return LowLevelAPI.Snapshots.putRule(token, user, project, snapshot, config, ruleId, ruleBody, comment, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_RULE = new CliRestCommand('delete_rule', 'delete configuration rule') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleId = System.console().readLine("Rule id: ")
            return LowLevelAPI.Snapshots.deleteRule(token, user, project, snapshot, config, ruleId, hostPrefix)
        }
    }

    static final CliRestCommand EXPORT_CONFIGURATION = new CliRestCommand('export_config', 'export a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.exportConfiguration(token, user, project, snapshot, config, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            String conf = LowLevelAPI.Responses.asString(entity)
            File f = File.createTempFile("exported-configuration", null)
            f.text = conf
            println "Configuration written to ${f}"
            return conf
        }
    }

    static final CliRestCommand ANALYZE = new CliRestCommand('analyze', 'run an analysis on a code snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String profile = readProfileFromConsole(cliOptions)
            List<String> options = readOptionsFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.analyze(token, user, project, snapshot, config, profile, options, hostPrefix)
        }
    }

    static final CliRestCommand RUNTIME = new CliRestCommand('runtime', 'show runtime stats for an analysis') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getRuntime(token, user, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand GET_OUTPUT = new CliRestCommand('get_output', 'get an analysis output') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            String user = getUserName(cliOptions, false, hostPrefix)
            AuthToken token = getUserToken(user, cliOptions, true, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            String output = readOutputFromConsole(cliOptions)
            String start = readStartFromConsole(cliOptions)
            String count = readCountFromConsole(cliOptions)
            String appOnly = readAppOnlyFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getOutput(token, user, project, snapshot, config, analysisId, output, start, count, appOnly, hostPrefix)
        }
    }

    protected static String readProjectNameFromConsole(OptionAccessor cliOptions) {
        final String DEFAULT_PROJECT = 'test-project'
        String project = (cliOptions['project'] ?: null) as String
        if (project)
            println "Assuming project = ${project}"
        else
            project = System.console().readLine("Project (default: '${DEFAULT_PROJECT})': ")
        return ('' == project) ? DEFAULT_PROJECT : project
    }

    protected static final String getUserInfo(boolean askForCredentialsIfEmpty, String hostPrefix, CliAuthenticator.Selector selector) {
        String data = CliAuthenticator.getUserInfo(selector)
        if (!data && askForCredentialsIfEmpty) {
            //Ask for username and password
            LOGIN.execute(hostPrefix)
            data = CliAuthenticator.getUserInfo(selector)
        }
        data
    }

    protected static final AuthToken getUserToken(String user, OptionAccessor cliOptions, boolean askForCredentialsIfEmpty, String hostPrefix) {
        String token = cliOptions['token']
        String value = token ?: getUserInfo(askForCredentialsIfEmpty, hostPrefix, CliAuthenticator.Selector.TOKEN)
        return new AuthToken(user, value)
    }

    protected static final String getUserName(OptionAccessor cliOptions, boolean askForCredentialsIfEmpty, String hostPrefix) {
        String user = cliOptions['user'] ?: null
        if (user != null)
            return user
        else
            getUserInfo(askForCredentialsIfEmpty, hostPrefix, CliAuthenticator.Selector.USERNAME)
    }

    protected static final PostState getPostState(OptionAccessor cliOptions) {
        PostState postState = new PostState()
        readSnapshotInputsFromConsole(cliOptions).forEach { SnapshotInput input ->
            postState.addInput(input)
        }
        return postState
    }

    protected static final void printProjectOptions(String hostPrefix, String user, AuthToken token, String project) {
        Map<String, Object> options = Remote.at(hostPrefix, user, token).getProjectInputs(user, project)
        println()
        options.forEach{k, v ->
            println ("* Available options (${k}): " + v.collect {
                def opt = it as Map<String, Object>
                (opt.get('id') as String) + (opt.get('isFile') ? "@path" : "=value")
            })
        }
    }

    protected static List<String> readStacksFromConsole(OptionAccessor cliOptions) {
        return readOptionsFromConsole(cliOptions, 'stacks', 'Project stacks', Collections.singletonList('jvm'))
    }

    protected static List<String> readOptionsFromConsole(OptionAccessor cliOptions) {
        return readOptionsFromConsole(cliOptions, 'options', 'Analysis options', new ArrayList<String>())
    }

    protected static List<String> readOptionsFromConsole(OptionAccessor cliOptions, String pluralOpt,
                                                         String description, List<String> defaultValue) {
        Collection<String> values = (cliOptions[pluralOpt] ?: null) as Collection<String>
        if (values != null)
            println "Assuming ${pluralOpt} = ${values}"
        else
            values = System.console().readLine("${description} (separated by spaces, default: ${defaultValue}): ").trim().tokenize(' ')
        return (values ? new ArrayList<String>(values) : defaultValue)
    }

    protected static String readSnapshotNameFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'snapshot', 'Snapshot', 'snapshot1')
    }

    protected static String readProfileFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'profile', 'Analysis profile', 'r8')
    }

    protected static String readAnalysisIdFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'analysis', 'Analysis id')
    }

    protected static String readActionFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'action', 'Analysis action', 'stop')
    }

    protected static String readOutputFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'output', 'Output')
    }

    protected static String readStartFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'start', 'Start', '0')
    }

    protected static String readCountFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'count', 'Count', '10')
    }

    protected static String readUserFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'user', 'User', 'user')
    }

    protected static String readTokenFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'token', 'Authentication token', '')
    }

    protected static String readAppOnlyFromConsole(OptionAccessor cliOptions) {
        String ret = readOptionFromConsole(cliOptions, 'appOnly', 'Application-only results (true/false/null)', 'null')
        // Permit 'null' command line option.
        return ret == 'null' ? null : ret
    }

    protected static String readOptionFromConsole(OptionAccessor cliOptions, String name, String description,
                                                  String DEFAULT_VALUE = null) {
        String value = cliOptions ? (cliOptions[name] ?: null) : null
        if (value)
            println "Assuming ${name} = ${value}"
        else {
            String prompt = "${description} " + (DEFAULT_VALUE ? "(default: ${DEFAULT_VALUE})": "") + ": "
            value = System.console().readLine(prompt)
        }
        if ('' == value) {
            if (DEFAULT_VALUE)
                return DEFAULT_VALUE
            else
                throw new RuntimeException("ERROR: no ${name} given.")
        }
        return value
    }

    protected static String readSymbolFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'symbol', 'Symbol id')
    }

    protected static String readLineFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'line', 'Line number')
    }

    protected static String readFileFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'file', 'File')
    }

    protected static String readArtifactFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'artifact', 'Artifact')
    }

    protected static String readConfigFromConsole(OptionAccessor cliOptions, String defaultConfig = 'clyze.json') {
        return readOptionFromConsole(cliOptions, 'config', 'Configuration', defaultConfig)
    }

    protected static List<SnapshotInput> readSnapshotInputsFromConsole(OptionAccessor cliOptions) {
        List<SnapshotInput> inputs = new ArrayList<>()
        Collection<String> tokens = (cliOptions['inputs'] ?: null) as Collection<String>
        if (tokens != null)
            println "Assuming inputs = ${tokens}"
        else
            tokens = System.console().readLine("Inputs (separated by spaces, example: 'app@path.jar jvm_platform=java_8'): ").tokenize(' ') as Collection<String>
        for (String token : tokens) {
            int atIdx = token.indexOf('@')
            if (atIdx > 0)
                inputs.add(new SnapshotInput(token.substring(0, atIdx), true, token.substring(atIdx + 1)))
            else {
                int eqIdx = token.indexOf('=')
                if (eqIdx > 0)
                    inputs.add(new SnapshotInput(token.substring(0, eqIdx), false, token.substring(eqIdx + 1)))
                else
                    throw new RuntimeException('ERROR: Bad snapshot input: ' + token)
            }
        }
        return inputs
    }

//    private static Set<Option> convertJsonEncodedOptionsToCliOptions(Object json) {
//        if (!json?.results)
//            return new HashSet<>()
//        Set<Option> ret = new HashSet<>()
//        json.results.each { result ->
//            List<Option> opts = result.options.collect { option ->
//                String description = option.description
//                if (!description) {
//                    description = "<no description>"
//                }
//                if (option.validValues) {
//                    description = "${description}\nAllowed values: ${option.validValues.join(', ')}"
//                }
//                if (option.defaultValue) {
//                    description = "${description}\nDefault value: ${option.defaultValue}"
//                }
//                if (option.isMandatory) {
//                    description = "${description}\nMandatory option."
//                }
//                if (option.multipleValues) {
//                    description = "${description}\nRepeatable option."
//                }
//
//                Option o = new Option(null, option.id?.toLowerCase(), !option.isBoolean, description)
//                if (option.multipleValues) {
//                    o.setArgs(Option.UNLIMITED_VALUES)
//                    if (option.isFile) {
//                        o.setArgName("files")
//                    }
//                } else if (option.isFile) {
//                    o.setArgName("file")
//                }
//                return o
//            }
//            ret.addAll(opts)
//        }
//        return ret
//    }
}
