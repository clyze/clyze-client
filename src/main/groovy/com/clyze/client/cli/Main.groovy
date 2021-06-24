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
import static com.clyze.client.cli.CliRestCommand.CLONE_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.CREATE_PROJECT
import static com.clyze.client.cli.CliRestCommand.CREATE_SAMPLE_PROJECT
import static com.clyze.client.cli.CliRestCommand.DELETE_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.DELETE_PROJECT
import static com.clyze.client.cli.CliRestCommand.DELETE_RULE
import static com.clyze.client.cli.CliRestCommand.DELETE_RULES
import static com.clyze.client.cli.CliRestCommand.DELETE_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.EXPORT_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.GET_CODE_FILE
import static com.clyze.client.cli.CliRestCommand.GET_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.GET_FILE
import static com.clyze.client.cli.CliRestCommand.GET_FILES
import static com.clyze.client.cli.CliRestCommand.GET_OUTPUT
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT_ANALYSES
import static com.clyze.client.cli.CliRestCommand.GET_PROJECT_OPTIONS
import static com.clyze.client.cli.CliRestCommand.GET_RULES
import static com.clyze.client.cli.CliRestCommand.GET_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.GET_SNAPSHOT_OPTIONS
import static com.clyze.client.cli.CliRestCommand.GET_SYMBOL
import static com.clyze.client.cli.CliRestCommand.LIST_CONFIGURATIONS
import static com.clyze.client.cli.CliRestCommand.LIST_PROJECTS
import static com.clyze.client.cli.CliRestCommand.LIST_SAMPLES
import static com.clyze.client.cli.CliRestCommand.LIST_SNAPSHOTS
import static com.clyze.client.cli.CliRestCommand.LIST_STACKS
import static com.clyze.client.cli.CliRestCommand.LOGIN
import static com.clyze.client.cli.CliRestCommand.PASTE_CONFIGURATION_RULES
import static com.clyze.client.cli.CliRestCommand.PING
import static com.clyze.client.cli.CliRestCommand.POST_RULE
import static com.clyze.client.cli.CliRestCommand.POST_SAMPLE_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.POST_SNAPSHOT
import static com.clyze.client.cli.CliRestCommand.PUT_RULE
import static com.clyze.client.cli.CliRestCommand.RENAME_CONFIGURATION
import static com.clyze.client.cli.CliRestCommand.REPACKAGE
import static com.clyze.client.cli.CliRestCommand.RUNTIME

/**
 * A command line client for a remote doop server.
 *
 * The client can execute the following commands via the remote server:
 * <ul>
 *     <li>list_stacks       - list the available stacks
 *     <li>list_projects     - list the available projects
 *     <li>create_project    - create a project
 *     <li>create_sample_project-create a project based on a sample
 *     <li>get_project       - get a project
 *     <li>delete_project    - delete a project
 *     <li>get_project_options - show the options of a project
 *     <li>get_project_analyses - show the analyses supported by a project
 *
 *     <li>list_snapshots    - list the available snapshots
 *     <li>get_snapshot      - get a snapshot
 *     <li>get_snapshot_options - get snapshot options
 *     <li>post_snapshot     - create a new snapshot
 *     <li>delete_snapshot   - delete a snapshot
 *     <li>list_samples      - list the available sample snapshots
 *     <li>post_sample       - create a new snapshot, based on a given sample
 *     <li>get_symbol        - get a symbol from the snapshot
 *     <li>get_files         - read the snapshot (artifact) files
 *     <li>get_file          - read a snapshot (artifact) file
 *     <li>get_code_file     - read a snapshot (code) file
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
 */
@CompileStatic
@Log4j
class Main {

    /** The map of available commands. */
    public static final Map<String, CliRestCommand> COMMANDS = [
            // Projects
            LIST_PROJECTS, CREATE_PROJECT, CREATE_SAMPLE_PROJECT, GET_PROJECT, DELETE_PROJECT, GET_PROJECT_OPTIONS, GET_PROJECT_ANALYSES,
            // Snapshots
            LIST_SNAPSHOTS, LIST_SAMPLES, POST_SNAPSHOT, POST_SAMPLE_SNAPSHOT, GET_SNAPSHOT, GET_SNAPSHOT_OPTIONS, DELETE_SNAPSHOT,
            GET_SYMBOL, GET_FILE, GET_FILES, GET_CODE_FILE,
            // Configurations
            LIST_CONFIGURATIONS, GET_CONFIGURATION, CLONE_CONFIGURATION, RENAME_CONFIGURATION, DELETE_CONFIGURATION, EXPORT_CONFIGURATION, GET_RULES, POST_RULE, DELETE_RULES, PUT_RULE, DELETE_RULE, PASTE_CONFIGURATION_RULES,
            // Misc.
            PING, LOGIN, REPACKAGE, ANALYZE, GET_OUTPUT, RUNTIME, LIST_STACKS
            // POST_DOOP, POST_CCLYZER, LIST, GET, STOP, POST_PROCESS, RESET, RESTART, DELETE, QUICKSTART
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
                Remote remote = parseRemote(cli['r'] as String)

                if (!command) {
                    throw new RuntimeException("ERROR: 'command' not properly initialized in: ${cmd}")
                }

                CliAuthenticator.init()
                command.cliOptions = cli
                println command.execute(remote.host, remote.port)

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
        opts.addOption(Option.builder('r').longOpt('remote').numberOfArgs(1).argName('[hostname|ip]:[port]').desc('Give remote server.').build())
        opts.addOption(Option.builder('c').longOpt('command')
                .desc("The command to execute via the remote server. Available commands: ${availableCommands}.")
                .numberOfArgs(1).argName("command").build())
        opts.addOption(Option.builder().longOpt('version').desc('Display version and exit.').build())
        opts.addOption(Option.builder().longOpt('project').numberOfArgs(1).argName('NAME').desc('Give project name.').build())
        opts.addOption(Option.builder().longOpt('stack').numberOfArgs(1).argName('ID').desc('Give project stack id.').build())
        opts.addOption(Option.builder().longOpt('snapshot').numberOfArgs(1).argName('ID').desc('Give snapshot id.').build())
        opts.addOption(Option.builder().longOpt('input').numberOfArgs(1).argName('INPUT').desc('Give snapshot input (examples: app@path, key=value).').build())
        opts.addOption(Option.builder().longOpt('symbol').numberOfArgs(1).argName('ID').desc('Give symbol id.').build())
        opts.addOption(Option.builder().longOpt('artifact').numberOfArgs(1).argName('NAME').desc('Give snapshot artifact.').build())
        opts.addOption(Option.builder().longOpt('file').numberOfArgs(1).argName('NAME').desc('Give snapshot file.').build())
        cli.setOptions(opts)

        return cli
    }

    static String getAvailableCommands() {
        return COMMANDS.keySet().sort().join(', ')
    }

    static Remote parseRemote(String remoteDef) {        
        String[] parts = remoteDef.split(":")
        int len = parts.length
        if (len < 1 || len > 2) {
            throw new RuntimeException("The value of the remote option is invalid: $remoteDef")
        }

        String host = parts[0]
        int port
        try {
            port = parts[1] as int
        } catch(e) {
            port = 80
            println "Using default port number: $port"
            log.debug e.message
        }

        return new Remote(host:host, port:port)
    }

    private static final class Remote {
        String host
        int port                
    }
}
