package org.clyze.doop.web.client

import org.apache.commons.cli.Option

/**
 * A Rest Client command.
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 12/2/2015
 */
class CliRestCommand extends RestCommandBase<String>{

    /** The name of the command */
    String name

    /** The description of the command */
    String description

    /** The command line options supported by the command */
    List<Option> options = []

    /** The command line options the command has been actually invoked with */
    OptionAccessor cliOptions
}
