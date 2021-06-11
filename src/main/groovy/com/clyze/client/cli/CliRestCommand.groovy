package com.clyze.client.cli

import groovy.cli.commons.OptionAccessor
import org.apache.commons.cli.Option
import com.clyze.client.web.http.HttpClientCommand

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

    /** The command line options the command has been actually invoked with */
    OptionAccessor cliOptions
}
