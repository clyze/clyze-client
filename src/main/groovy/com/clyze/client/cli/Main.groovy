package com.clyze.client.cli

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.log4j.Logger
import org.clyze.utils.Helper
import org.clyze.utils.JHelper

import static com.clyze.client.cli.CliRestCommand.ANALYZE
import static com.clyze.client.cli.CliRestCommand.CLEAN_DEPLOY
import static com.clyze.client.cli.CliRestCommand.CLONE_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.CREATE_PROJECT
import static com.clyze.client.cli.CliRestCommand.DELETE_ANALYSIS
import static com.clyze.client.cli.CliRestCommand.DELETE_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.DELETE_PROJECT
import static com.clyze.client.cli.CliRestCommand.DELETE_RULE
import static com.clyze.client.cli.CliRestCommand.DELETE_RULES
import static com.clyze.client.cli.CliRestCommand.DELETE_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.DIAGNOSE
import static com.clyze.client.cli.CliRestCommand.EXECUTE_ANALYSIS_ACTION
import static com.clyze.client.cli.CliRestCommand.EXPORT_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.GET_ANALYSIS
import static com.clyze.client.cli.CliRestCommand.GET_ANALYSIS_RUNTIME
import static com.clyze.client.cli.CliRestCommand.GET_CODE_FILE
import static com.clyze.client.cli.CliRestCommand.GET_CODE_HINTS
import static com.clyze.client.cli.CliRestCommand.GET_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.GET_FILE
import static com.clyze.client.cli.CliRestCommand.GET_FILES
import static com.clyze.client.cli.CliRestCommand.GET_OUTPUT
import static com.clyze.client.cli.CliRestCommand.GET_OUTPUT_FILE
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT_ANALYSES
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT_INPUTS
import static com.clyze.client.cli.CliRestCommand.GET_RULES
import static com.clyze.client.cli.CliRestCommand.GET_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.GET_SYMBOL
import static com.clyze.client.cli.CliRestCommand.GET_SYMBOLS
import static com.clyze.client.cli.CliRestCommand.LIST_CONFIGURATIONS
import static com.clyze.client.cli.CliRestCommand.LIST_PROJECTS
import static com.clyze.client.cli.CliRestCommand.LIST_PUBLIC_PROJECTS
import static com.clyze.client.cli.CliRestCommand.LIST_SNAPSHOTS
import static com.clyze.client.cli.CliRestCommand.LIST_STACKS
import static com.clyze.client.cli.CliRestCommand.LIST_USERS
import static com.clyze.client.cli.CliRestCommand.LOGIN
import static com.clyze.client.cli.CliRestCommand.PASTE_CONFIGURATION_RULES
import static com.clyze.client.cli.CliRestCommand.PING
import static com.clyze.client.cli.CliRestCommand.POST_RULE
import static com.clyze.client.cli.CliRestCommand.POST_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.PUT_RULE
import static com.clyze.client.cli.CliRestCommand.RENAME_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.REPACKAGE
import static com.clyze.client.cli.CliRestCommand.RUNTIME

/**
 * A command line client for a remote analysis server.
 *
 * The client can execute the following commands via the remote server:
 * <ul>
 *     <li>list_stacks       - list the available stacks
 *     <li>list_public_projects - list all public projects
 *     <li>list_projects     - list the available projects
 *     <li>create_project    - create a project
 *     <li>get_project       - get a project
 *     <li>delete_project    - delete a project
 *     <li>get_project_inputs - show the inputs supported by a project
 *     <li>get_project_analyses - show the analyses supported by a project
 *
 *     <li>list_snapshots    - list the available snapshots
 *     <li>get_snapshot      - get a snapshot
 *     <li>post_snapshot     - create a new snapshot
 *     <li>delete_snapshot   - delete a snapshot
 *     <li>get_symbol        - get a symbol from the snapshot
 *     <li>get_symbols       - get the symbols of a given line
 *     <li>get_files         - read the snapshot (artifact) files
 *     <li>get_file          - read a snapshot (artifact) file
 *     <li>get_code_file     - read a snapshot (code) file
 *     <li>get_code_hints    - read the hints for a snapshot (code) file
 *     <li>get_output_file   - read an analysis output file
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
 *     <li>analyze           - create and run an analysis
 *     <li>get_analysis      - read a snapshot analysis
 *     <li>get_analysis_runtime - read snapshot analysis runtime information
 *     <li>delete_analysis   - delete a snapshot analysis
 *     <li>execute_action    - execute a snapshot analysis action
 *
 *     <li>clean_deploy      - clean a server for deployment
 *     <li>login             - authenticate user
 *     <li>list_users        - list users
 *     <li>ping              - check connection with server
 *     <li>diagnose          - invoke the "diagnose" endpoint
 *     <li>repackage         - run automated repackaging
 *     <li>runtime           - check the runtime status of an analysis
 *     <li>list              - list the available analyses
 *     <li>get               - retrieves an analysis
 *     <li>stop              - stop an analysis
 *     <li>query             - query a complete analysis
 *     <li>delete            - delete an analysis
 * </ul>
 */
@CompileStatic
@Log4j
class Main {

    /** The map of available commands. */
    public static final Map<String, CliRestCommand> COMMANDS = [
            // Projects
            LIST_PUBLIC_PROJECTS, LIST_PROJECTS, CREATE_PROJECT, GET_PROJECT, DELETE_PROJECT,
            GET_PROJECT_ANALYSES, GET_PROJECT_INPUTS,
            // Snapshots
            LIST_SNAPSHOTS, POST_SNAPSHOT, GET_SNAPSHOT, DELETE_SNAPSHOT, GET_SYMBOLS,
            GET_SYMBOL, GET_FILE, GET_FILES, GET_CODE_FILE, GET_OUTPUT_FILE, GET_OUTPUT,
            GET_CODE_HINTS,
            // Configurations
            LIST_CONFIGURATIONS, GET_CONFIGURATION, CLONE_CONFIGURATION, RENAME_CONFIGURATION, DELETE_CONFIGURATION, EXPORT_CONFIGURATION, GET_RULES, POST_RULE, DELETE_RULES, PUT_RULE, DELETE_RULE, PASTE_CONFIGURATION_RULES,
            // Analyses
            ANALYZE, GET_ANALYSIS, DELETE_ANALYSIS, EXECUTE_ANALYSIS_ACTION, GET_ANALYSIS_RUNTIME,
            // Misc.
            CLEAN_DEPLOY, PING, LOGIN, LIST_USERS, REPACKAGE, RUNTIME, LIST_STACKS, DIAGNOSE
            // LIST, GET, STOP, POST_PROCESS, RESET, RESTART, DELETE, QUICKSTART
    ].collectEntries {
        [(it.name):it]
    }

    /**
     * The entry point.
     */
    static void main(String[] args) {

        try {

            JHelper.initConsoleLogging("DEBUG")

            CliBuilder builder = createCliBuilder()
            OptionAccessor cli = builder.parse(args)
            String cmd = null
            CliRestCommand command = null

            if (!cli || !args) {
                builder.usage()
                return
            }

            if (cli['v']) {
                println JHelper.getVersionInfo(Main.class)
                return
            }

            if (cli['c']) {
                cmd = cli['c']
                command = COMMANDS.get(cmd.toLowerCase())
                if (!command) {
                    System.out.println "The value of the command option is invalid: '${cmd}'. Available commands: ${availableCommands}."
                    exitWithError()
                }
            }
            if (cli['h']) {
                if (command)
                    println "${command.name} - ${command.description}"
                else
                    builder.usage()
                return
            }

            if (cli['r']) {
                String hostPrefix = cli['r'] as String

                if (!command) {
                    throw new RuntimeException("ERROR: 'command' not properly initialized in: ${cmd}")
                }

                CliAuthenticator.init()
                command.cliOptions = cli
                println command.execute(hostPrefix)

            } else {
                builder.usage()
                //noinspection GroovyUnnecessaryReturn
                return
            }
        } catch (e) {
            println e.message
            if (Logger.getRootLogger().isDebugEnabled()) {
                println Helper.stackTraceToString(e)
            }
            exitWithError()
        }
    }

    private static void exitWithError() {
        System.exit(-1)
    }

    private static final CliBuilder createCliBuilder() {
        CliBuilder cli = new CliBuilder(
            parser: new DefaultParser (),
            usage : "client -r [remote] -c [command].",
            width : 120
        )        

        Options opts = new Options()
        opts.addOption(Option.builder('h').longOpt('help')
                .desc('Display help and exit. Combine it with a command to see the command options.').build())
        opts.addOption(Option.builder('r').longOpt('remote').numberOfArgs(1).argName('[hostname|ip]:[port][/path]').desc('Give remote server.').build())
        opts.addOption(Option.builder('c').longOpt('command')
                .desc("The command to execute via the remote server. Available commands: ${availableCommands}.")
                .numberOfArgs(1).argName("command").build())
        opts.addOption(Option.builder('v').longOpt('version').desc('Display version and exit.').build())
        opts.addOption(Option.builder().longOpt('project').numberOfArgs(1).argName('NAME').desc('Set project name.').build())
        opts.addOption(Option.builder().longOpt('stack').numberOfArgs(1).argName('ID').desc('Set project stack (by id).').build())
        opts.addOption(Option.builder().longOpt('public').desc('Make project public.').build())
        opts.addOption(Option.builder().longOpt('snapshot').numberOfArgs(1).argName('ID').desc('set snapshot id.').build())
        opts.addOption(Option.builder().longOpt('input').numberOfArgs(1).argName('INPUT').desc('Set snapshot input (examples: app@path, key=value).').build())
        opts.addOption(Option.builder().longOpt('symbol').numberOfArgs(1).argName('ID').desc('Set symbol id.').build())
        opts.addOption(Option.builder().longOpt('artifact').numberOfArgs(1).argName('NAME').desc('Set snapshot artifact.').build())
        opts.addOption(Option.builder().longOpt('file').numberOfArgs(1).argName('NAME').desc('Set snapshot file.').build())
        opts.addOption(Option.builder().longOpt('profile').numberOfArgs(1).argName('ID').desc('Set analysis profile (by id).').build())
        opts.addOption(Option.builder().longOpt('analysis').numberOfArgs(1).argName('ID').desc('Set snapshot analysis (by id).').build())
        opts.addOption(Option.builder().longOpt('config').numberOfArgs(1).argName('NAME').desc('Set snapshot configuration.').build())
        opts.addOption(Option.builder().longOpt('action').numberOfArgs(1).argName('ACTION').desc('Set action to execute.').build())
        opts.addOption(Option.builder().longOpt('output').numberOfArgs(1).argName('ID').desc('Set analysis output (such as a dataset).').build())
        opts.addOption(Option.builder().longOpt('start').numberOfArgs(1).argName('N').desc('Set start position when reading data.').build())
        opts.addOption(Option.builder().longOpt('count').numberOfArgs(1).argName('N').desc('Set element count when reading data.').build())
        opts.addOption(Option.builder().longOpt('option').numberOfArgs(1).argName('OPT').desc('Set analysis option in the form "id=value".').build())
        opts.addOption(Option.builder().longOpt('user').numberOfArgs(1).argName('USER').desc('Set user name.').build())
        opts.addOption(Option.builder().longOpt('token').numberOfArgs(1).argName('TOKEN').desc('Set authentication token.').build())
        opts.addOption(Option.builder().longOpt('appOnly').numberOfArgs(1).argName('FLAG').desc('Set "appOnly" filter (true/false).').build())
        opts.addOption(Option.builder().longOpt('line').numberOfArgs(1).argName('LINE').desc('Set code line number.').build())
        cli.setOptions(opts)

        return cli
    }

    static String getAvailableCommands() {
        return COMMANDS.keySet().sort().join(', ')
    }
}
