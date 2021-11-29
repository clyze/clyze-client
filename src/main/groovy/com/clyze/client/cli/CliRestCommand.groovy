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
    static final AuthToken readLogin(OptionAccessor cliOptions) {
        String user = readAuthUserFromConsole(cliOptions)
        String tokenValue = readAuthTokenFromConsole(cliOptions)
        LOGIN_LAST_USERNAME = user
        return new AuthToken(user, tokenValue)
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
            AuthToken token = readLogin(cliOptions)
            return LowLevelAPI.Requests.login(token, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            String token = LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "token")
            CliAuthenticator.setUserInfo(LOGIN_LAST_USERNAME, token)
            return "Logged in, token updated."
        }
    }

    static final CliRestCommand CREATE_USER = new CliRestCommand('create_user', 'create a user') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String newUserId = readOptionFromConsole(cliOptions, 'user-id', 'New user id', null)
            String newUserName = readOptionFromConsole(cliOptions, 'user', 'New user name', null)
            String newUserPassword = readOptionFromConsole(cliOptions, 'user-pass', 'New user password', null)
            return LowLevelAPI.Users.createUser(token, newUserId, newUserName, newUserPassword, hostPrefix)
        }
    }

    static final CliRestCommand LIST_USERS = new CliRestCommand('list_users', 'list the users') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            return LowLevelAPI.Users.listUsers(token, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_USER = new CliRestCommand('delete_user', 'delete user') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String userToDelete = readOptionFromConsole(cliOptions, 'user', 'User to delete (name)', null)
            String userIdToDelete = readOptionFromConsole(cliOptions, 'user-id', 'User to delete (id)', null)
            return LowLevelAPI.Users.deleteUser(token, userIdToDelete, userToDelete, hostPrefix)
        }
    }

    static final CliRestCommand LIST_SNAPSHOTS = new CliRestCommand('list_snapshots',  'list the snapshots stored in server') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.listSnapshots(token, owner, project, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            return LowLevelAPI.Projects.getProjects(token, owner, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            List<String> stacks = readStacksFromConsole(cliOptions)
            String isPublic = readOptionFromConsole(cliOptions, 'public', 'Project is public (true/false)', 'false')
            return LowLevelAPI.Projects.createProject(token, owner, project, stacks, isPublic, hostPrefix)
        }
    }

    static final CliRestCommand GET_PROJECT_INPUTS = new CliRestCommand('get_project_inputs', 'get input options of project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProjectInputs(token, owner, project, hostPrefix)
        }
    }

    static final CliRestCommand GET_PROJECT_ANALYSES = new CliRestCommand('get_project_analyses', 'get the analyses supported by a project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProjectAnalyses(token, owner, project, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.getProject(token, owner, project, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_PROJECT = new CliRestCommand('delete_project', 'delete project') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            return LowLevelAPI.Projects.deleteProject(token, owner, project, hostPrefix)
        }
    }

    static final CliRestCommand GET_SNAPSHOT = new CliRestCommand('get_snapshot', 'read a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getSnapshot(token, owner, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand GET_SYMBOL = new CliRestCommand('get_symbol', 'read a symbol from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String symbol = readSymbolFromConsole(cliOptions, 'Symbol id')
            return LowLevelAPI.Snapshots.getSymbol(token, owner, project, snapshot, symbol, hostPrefix)
        }
    }

    static final CliRestCommand GET_SYMBOLS = new CliRestCommand('get_symbols', 'get the symbols of a given code line') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String line = readLineFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Requests.getSymbols(token, owner, project, snapshot, config, file, line, hostPrefix)
        }
    }

    static final CliRestCommand SEARCH_SYMBOL = new CliRestCommand('search_symbol', 'search a symbol from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String symbol = readSymbolFromConsole(cliOptions, 'Symbol token')
            String prefix = readOptionFromConsole(cliOptions, 'prefix', 'Search by prefix?', 'true')
            return LowLevelAPI.Snapshots.searchSymbol(token, owner, project, snapshot, symbol, prefix, hostPrefix)
        }
    }

    static final CliRestCommand GET_FILES = new CliRestCommand('get_files', 'read the snapshot files') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String artifact = readArtifactFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getFiles(token, owner, project, snapshot, artifact, hostPrefix)
        }
    }

    static final CliRestCommand GET_FILE = new CliRestCommand('get_file', 'read a snapshot file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String artifact = readArtifactFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getFile(token, owner, project, snapshot, artifact, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_CODE_HINTS = new CliRestCommand('get_code_hints', 'get the hints for a snapshot code file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getCodeFileHints(token, owner, project, snapshot, config, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_CODE_FILE = new CliRestCommand('get_code_file', 'read a snapshot code file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getCodeFile(token, owner, project, snapshot, file, hostPrefix)
        }
    }

    static final CliRestCommand GET_OUTPUT_FILE = new CliRestCommand('get_output_file', 'read an analysis output file') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysis = readAnalysisIdFromConsole(cliOptions)
            String file = readFileFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysisOutputFile(token, owner, project, snapshot, config, analysis, file, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            printProjectOptions(hostPrefix, owner, token, project)
            PostState postState = getPostState(cliOptions)
            return LowLevelAPI.Snapshots.createSnapshot(token, owner, project, postState, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
        }
    }

    static final CliRestCommand REPACKAGE = new CliRestCommand('repackage', 'automated repackaging endpoint') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            PostState postState = getPostState(cliOptions)
            return LowLevelAPI.Projects.repackageSnapshotForCI(token, owner, project, postState, hostPrefix)
        }

        @Override
        String onSuccess(HttpEntity entity) {
            return LowLevelAPI.Responses.parseJsonAndGetAttr(entity, "id") as String
        }
    }

    static final CliRestCommand DELETE_SNAPSHOT = new CliRestCommand('delete_snapshot', 'delete snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteSnapshot(token, owner, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand LIST_CONFIGURATIONS = new CliRestCommand('list_configurations', 'list the configurations of a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.listConfigurations(token, owner, project, snapshot, hostPrefix)
        }
    }

    static final CliRestCommand GET_ANALYSIS = new CliRestCommand('get_analysis', 'reads an analysis from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysis(token, owner, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand PUT_ANALYSIS = new CliRestCommand('put_analysis', 'updates an analysis in a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            List<String> options = readOptionsFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.putAnalysis(token, owner, project, snapshot, config, analysisId, options, hostPrefix)
        }
    }

    static final CliRestCommand GET_ANALYSIS_RUNTIME = new CliRestCommand('get_analysis_runtime', 'read analysis runtime information') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getAnalysisRuntime(token, owner, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_ANALYSIS = new CliRestCommand('delete_analysis', 'deletes an analysis from a snapshot') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteAnalysis(token, owner, project, snapshot, config, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand EXECUTE_ANALYSIS_ACTION = new CliRestCommand('execute_action', 'executes an analysis action (such as "stop" or "restart")') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            String action = readActionFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.executeAnalysisAction(token, owner, project, snapshot, config, action, analysisId, hostPrefix)
        }
    }

    static final CliRestCommand GET_CONFIGURATION = new CliRestCommand('get_config', 'get a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getConfiguration(token, owner, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand CLONE_CONFIGURATION = new CliRestCommand('clone_config', 'clone a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.cloneConfiguration(token, owner, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand RENAME_CONFIGURATION = new CliRestCommand('rename_config', 'rename a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String newName = readConfigFromConsole(null, 'new-name.json')
            return LowLevelAPI.Snapshots.renameConfiguration(token, owner, project, snapshot, config, newName, hostPrefix)
        }
    }

    static final CliRestCommand PASTE_CONFIGURATION_RULES = new CliRestCommand('paste_rules', 'pastes rules from a snapshot configuration to another configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String fromConfig = readConfigFromConsole(null, 'config2.json')
            return LowLevelAPI.Snapshots.pasteConfigurationRules(token, owner, project, snapshot, config, fromConfig, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_CONFIGURATION = new CliRestCommand('delete_config', 'delete a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteConfiguration(token, owner, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_RULES = new CliRestCommand('delete_rules', 'delete configuration rules') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            List<String> ids = readOptionsFromConsole(cliOptions, 'rule-ids', 'Rule ids', new ArrayList<String>())
            return LowLevelAPI.Snapshots.deleteRules(token, owner, project, snapshot, config, ids, hostPrefix)
        }
    }

    static final CliRestCommand GET_RULES = new CliRestCommand('get_rules', 'get configuration rules') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String originType = readOptionFromConsole(cliOptions, 'origin', 'Origin type', '', true)
            if (originType == '')
                originType = null
            String start = readStartFromConsole(cliOptions)
            Integer _start = start == '' ? null : Integer.valueOf(start)
            String count = readCountFromConsole(cliOptions)
            Integer _count = count == '' ? null : Integer.valueOf(count)
            String facets = readOptionFromConsole(cliOptions, 'facets', 'Facets', 'false')
            return LowLevelAPI.Snapshots.getRules(token, owner, project, snapshot, config, originType, _start, _count, facets, hostPrefix)
        }
    }

    static final CliRestCommand POST_RULE = new CliRestCommand('post_rule', 'post configuration rule') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleBody = readRuleBodyFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.postRule(token, owner, project, snapshot, config, ruleBody, null, hostPrefix)
        }
    }

    static final CliRestCommand PUT_RULE = new CliRestCommand('put_rule', 'put configuration rule (e.g. edit comment)') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleId = readRuleIdFromConsole(cliOptions)
            String ruleBody = readRuleBodyFromConsole(cliOptions)
            String comment = readOptionFromConsole(cliOptions, 'rule-comment', 'Rule comment', null)
            return LowLevelAPI.Snapshots.putRule(token, owner, project, snapshot, config, ruleId, ruleBody, comment, hostPrefix)
        }
    }

    static final CliRestCommand DELETE_RULE = new CliRestCommand('delete_rule', 'delete configuration rule') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String ruleId = readRuleIdFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.deleteRule(token, owner, project, snapshot, config, ruleId, hostPrefix)
        }
    }

    static final CliRestCommand EXPORT_CONFIGURATION = new CliRestCommand('export_config', 'export a snapshot configuration') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.exportConfiguration(token, owner, project, snapshot, config, hostPrefix)
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
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String profile = readProfileFromConsole(cliOptions)
            List<String> options = readOptionsFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.analyze(token, owner, project, snapshot, config, profile, options, hostPrefix)
        }
    }

    static final CliRestCommand RUNTIME = new CliRestCommand('runtime', 'show runtime stats for an analysis') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getRuntime(token, owner, project, snapshot, config, hostPrefix)
        }
    }

    static final CliRestCommand GET_OUTPUT = new CliRestCommand('get_output', 'get an analysis output') {
        @Override
        HttpUriRequest buildRequest(String hostPrefix) {
            AuthToken token = getUserAuthToken(cliOptions, hostPrefix)
            String owner = readOwnerFromConsole(cliOptions, hostPrefix)
            String project = readProjectNameFromConsole(cliOptions)
            String snapshot = readSnapshotNameFromConsole(cliOptions)
            String config = readConfigFromConsole(cliOptions)
            String analysisId = readAnalysisIdFromConsole(cliOptions)
            String output = readOutputFromConsole(cliOptions)
            String start = readStartFromConsole(cliOptions)
            String count = readCountFromConsole(cliOptions)
            String appOnly = readAppOnlyFromConsole(cliOptions)
            return LowLevelAPI.Snapshots.getOutput(token, owner, project, snapshot, config, analysisId, output, start, count, appOnly, hostPrefix)
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

    protected static final AuthToken getUserAuthToken(OptionAccessor cliOptions, String hostPrefix) {
        String user = readAuthUserFromConsole(cliOptions, true)
        String token = readAuthTokenFromConsole(cliOptions, true)
        if (user == null) {
            if (token != null)
                throw new RuntimeException('ERROR: authentication token given without a user name')
            user = getUserInfo(true, hostPrefix, CliAuthenticator.Selector.USERNAME)
            token = getUserInfo(true, hostPrefix, CliAuthenticator.Selector.TOKEN)
        }
        return new AuthToken(user, token)
    }

    protected static final String readOwnerFromConsole(OptionAccessor cliOptions, String hostPrefix) {
        String cachedUser = getUserInfo(false, hostPrefix, CliAuthenticator.Selector.USERNAME)
        return readOptionFromConsole(cliOptions, 'user', 'User', cachedUser)
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

    protected static String readAuthUserFromConsole(OptionAccessor cliOptions, boolean optional = false) {
        return readOptionFromConsole(cliOptions, 'auth-user', 'User', null, optional)
    }

    protected static String readAuthTokenFromConsole(OptionAccessor cliOptions, boolean optional = false) {
        return readOptionFromConsole(cliOptions, 'auth-token', 'Authentication token', null, optional)
    }

    protected static String readAppOnlyFromConsole(OptionAccessor cliOptions) {
        String ret = readOptionFromConsole(cliOptions, 'appOnly', 'Application-only results (true/false/null)', 'null')
        // Permit 'null' command line option.
        return ret == 'null' ? null : ret
    }

    protected static String readOptionFromConsole(OptionAccessor cliOptions, String name, String description,
                                                  String DEFAULT_VALUE = null, boolean optional = false) {
        String value = cliOptions ? (cliOptions[name] ?: null) : null
        if (!value && !optional) {
            String prompt = "${description} " + (DEFAULT_VALUE ? "(default: ${DEFAULT_VALUE})": "") + ": "
            value = System.console().readLine(prompt)
            if ('' == value) {
                if (DEFAULT_VALUE)
                    return DEFAULT_VALUE
                else
                    throw new RuntimeException("ERROR: no ${name} given.")
            }
        }
        println "Using ${name} = ${value}"
        return value
    }

    protected static String readSymbolFromConsole(OptionAccessor cliOptions, String description) {
        return readOptionFromConsole(cliOptions, 'symbol', description)
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

    protected static String readRuleIdFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'rule-id', 'Rule id', null)
    }

    protected static String readRuleBodyFromConsole(OptionAccessor cliOptions) {
        return readOptionFromConsole(cliOptions, 'rule-body', 'Rule body', null)
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
