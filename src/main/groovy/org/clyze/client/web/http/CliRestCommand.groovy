package org.clyze.client.web.http

import org.apache.commons.cli.Option

/**
 * A CLI Rest Client command.
 */
class CliRestCommand extends HttpClientCommand<String> {    

    static CliRestCommand extend(Map<String, Object> attrsToOverride=[:], HttpClientCommand cmd) {
        def attrs = ["httpClientLifeCycle", "requestBuilder", "onSuccess", "onError"]
        def map = extendMap(attrs, attrsToOverride, cmd)            

        map.name = attrsToOverride.name
        map.description = attrsToOverride.description
        map.options = attrsToOverride.options        

        return new CliRestCommand(map)
    }

    /** The name of the command */
    String name

    /** The description of the command */
    String description

    /** A closure that returns the command line options supported by the command */
    Closure<List<Option>> optionsBuilder = { String host, int port -> [] }

    /** The command line options the command has been actually invoked with */
    OptionAccessor cliOptions

    private List<Option> supportedOptions = null

    List<Option> discoverOptions(String host, int port) {
        supportedOptions = optionsBuilder ? optionsBuilder.call(host, port) : []
        supportedOptions
    }
}
