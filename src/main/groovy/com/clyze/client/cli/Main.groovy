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

@CompileStatic
@Log4j
class Main {

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
                command = CliRestClient.COMMANDS.get(cmd.toLowerCase())
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
        opts.addOption(Option.builder().longOpt('discover').desc('Show discovered options.').build())
        opts.addOption(Option.builder().longOpt('version').desc('Display version and exit.').build())
        opts.addOption(Option.builder().longOpt('project').numberOfArgs(1).argName('NAME').desc('Give project name.').build())
        opts.addOption(Option.builder().longOpt('snapshot').numberOfArgs(1).argName('ID').desc('Give snapshot id.').build())
        opts.addOption(Option.builder().longOpt('input').numberOfArgs(1).argName('INPUT').desc('Give snapshot input (examples: app@path, key=value).').build())
        cli.setOptions(opts)

        return cli
    }

    static String getAvailableCommands() {
        return CliRestClient.COMMANDS.keySet().sort().join(', ')
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
