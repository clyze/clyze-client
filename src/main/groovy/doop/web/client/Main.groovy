package doop.web.client
import doop.core.AnalysisOption
import doop.core.Doop
import doop.core.Helper
import org.apache.commons.cli.Option
/**
 * The entry point for the jdoop Restful client (@see doop.web.client.RestClient).
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 27/11/2014
 */
class Main {

    /**
     * The entry point.
     */
    static void main(String[] args) {

        try {

            //initialize logging (required to avoid log4j warning messages)
            Helper.initConsoleLogging("WARN")

            CliBuilder builder = createCliBuilder()
            OptionAccessor cli = builder.parse(args)
            String cmd
            RestCommand command


            if (!cli || !args) {
                builder.usage()
                return
            }

            if (cli.c) {
                cmd = cli.c
                command = RestClient.COMMANDS.find { it.name == cmd.toLowerCase()}
                if (!command) {
                    throw new RuntimeException("The value of the command option is invalid: $cmd")
                }
            }

            if (cli.h) {
                if (command) {
                    println "${command.name} - ${command.description}"
                    if (command.options) {
                        CliBuilder builder2 = new CliBuilder(usage: "-r [remote] -c ${command.name} [OPTION]...")
                        builder2.width = 120
                        command.options.each { Option option -> builder2 << option }
                        builder2.usage()
                    }
                    return
                }
                else {
                    builder.usage()
                    return
                }
            }

            if (cli.r) {
                String remote = cli.r
                String[] parts = remote.split(":")
                int len = parts.length
                if (len < 1 || len > 2) {
                    throw new RuntimeException("The value of the remote option is invalid: $remote")
                }

                String host = parts[0]
                int port = 8000
                try {
                    port = parts[1] as int
                }
                catch(all) {
                    println "Using default port number: $port"
                }

                if (command.options) {
                    command.options.each { Option option -> builder << option }
                    //reparse the args
                    cli = builder.parse(args)
                }

                println command.execute(host, port, cli)
            }

        } catch (e) {
            println e.getMessage()
            println Helper.stackTraceToString(e)
            System.exit(-1)
        }
    }

    private static final CliBuilder createCliBuilder() {
        CliBuilder cli = new CliBuilder(
            usage: "client -r [remote] -c [command]"
        )
        cli.width = 120

        cli.with {
            h(longOpt: 'help', "Display help and exit. Combine it with a command to see the command options.")
            r(longOpt: 'remote', "The remote doop server.", args:1, argName: "[hostname|ip]:[port]")
            c(longOpt: 'command', "The command to execute via the remote doop server. Available commands: \
                                  ping, list, post, get, start, stop, query.", args:1, argName: "command")
        }

        return cli
    }

    private static final void extendCliBuilderForCreateCommand(CliBuilder cli) {
        List<AnalysisOption> clientOptions = Doop.ANALYSIS_OPTIONS.findAll { AnalysisOption option ->
            option.webUI //all options with webUI property
        }
        Helper.addAnalysisOptionsToCliBuilder(clientOptions, cli)
    }
}
