package doop.web.client

import org.apache.commons.cli.Option
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntity
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
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
