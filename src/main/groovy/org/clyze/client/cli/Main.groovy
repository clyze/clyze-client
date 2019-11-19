package org.clyze.client.cli

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import groovy.util.logging.Log4j
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.log4j.Logger
import org.clyze.utils.Helper

@Log4j
class Main {

    /**
     * The entry point.
     */
    static void main(String[] args) {

        try {

            Helper.initConsoleLogging("DEBUG")            

            CliBuilder builder = createCliBuilder()
            OptionAccessor cli = builder.parse(args)
            String cmd = null
            CliRestCommand command = null

            if (!cli || !args) {
                builder.usage()
                return
            }

            if (cli.c) {
                cmd = cli.c
                command = CliRestClient.COMMANDS.get(cmd.toLowerCase())
                if (!command) {
                    println "The value of the command option is invalid: '${cmd}'. Available commands: ${availableCommands}."
                    exitWithError()
                }
            }
            if (cli.h) {
                if (command) {                    
                    List<Option> options = []
                    if (cli.r) {                        
                        Remote remote = parseRemote(cli.r as String)
                        options = command.discoverOptions(remote.host, remote.port)
                    }
                    println "${command.name} - ${command.description}"

                    if (options) {
                        CliBuilder builder2 = new CliBuilder(usage: "-r [remote] -c ${command.name} [OPTION]...")
                        builder2.width = 120
                        options.each { Option option -> builder2 << option }
                        builder2.usage()
                    } else {
                        println "Provide a valid remote to help the client dynamically discover the options supported by the command."
                    }
                    return
                } else {
                    builder.usage()
                    return
                }
            }

            if (cli.r) {
                Remote remote = parseRemote(cli.r as String)

                if (!command) {
                    throw new RuntimeException("ERROR: 'command' not properly initialized in: ${cmd}")
                }
                List<Option> options = command.discoverOptions(remote.host, remote.port)
                if (options) {
                    options.each { Option option -> builder << option }
                    //reparse the args
                    cli = builder.parse(args)
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
            footer: "Note that some commands may require -r to be present in order to discover options dynamically.",
            width : 120
        )        

        cli.with {
            h(longOpt: 'help', "Display help and exit. Combine it with a command to see the command options.")
            r(longOpt: 'remote', "The remote server.", args:1, argName: "[hostname|ip]:[port]")
            c(longOpt: 'command', "The command to execute via the remote server. Available commands: \
                                  ${availableCommands}.", args:1, argName: "command")
        }

        return cli
    }

    static String getAvailableCommands() {
        return CliRestClient.COMMANDS.keySet().join(', ')
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
