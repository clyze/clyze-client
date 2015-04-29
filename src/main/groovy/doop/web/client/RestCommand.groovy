package doop.web.client

import org.apache.commons.cli.Option
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntity
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
/**
 * A Rest Client command.
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 12/2/2015
 */
class RestCommand {

    public static final String BASE_PATH = "/jdoop/web/api/v1/"
    public static final String HEADER_TOKEN = "X-DOOP-TOKEN"

    private static final Closure<String> DEFAULT_SUCCES = { HttpEntity entity ->
        return "OK"
    }

    private static final Closure<String> DEFAULT_ERROR = { int statusCode, HttpEntity entity ->
        String message = entity ? EntityUtils.toString(entity) : "No message"
        return "Error $statusCode: $message"
    }

    private static final Closure<HttpUriRequest> DEFAULT_REQUEST_BUILDER = { String url, OptionAccessor cliOptions ->
        return new HttpGet(url)
    }

    private Log logger = LogFactory.getLog(getClass())

    /** The name of the command */
    String name

    /** The description of the command */
    String description

    /** The command line options supported by the command */
    List<Option> options = []

    /** The Restful endpoint of the command (suffix to the {@code BASE_PATH}.*/
    String endPoint = "analyses"

    /** Indication whether the command requires authentication */
    boolean authenticationRequired = true

    /** A closure that creates HttpUriRequest objects.
     * It accepts two parameters: (a) the url and (b) the OptionAccessor directly from the command line.
     */
    Closure<HttpUriRequest> buildRequest = DEFAULT_REQUEST_BUILDER

    /** A closure to be executed in case of success (status code == 200). It accepts an HttpEntity object and returns a
     * String.
     */
    Closure<String> onSuccess = DEFAULT_SUCCES

    /** A closure to be executed in case of error (status code != 200). It accepts the status code and an HttpEntity
     * object and return the error message.
     */
    Closure<String> onError = DEFAULT_ERROR

    String execute(String host, int port, OptionAccessor cliOptions) {
        String url = "http://${host}:${port}${BASE_PATH}${endPoint}"
        CloseableHttpClient client = HttpClients.createDefault()
        try {
            HttpUriRequest request = buildRequest.call(url, cliOptions)
            if (authenticationRequired) {
                String token = Authenticator.getUserToken()
                if (!token) {
                    //Ask for username and password
                    RestClient.COMMANDS.login.execute(host, port, null)
                    token = Authenticator.getUserToken()
                }

                //send the token with the request
                request.addHeader(HEADER_TOKEN, token)
            }

            logger.debug "Executing request: ${request.getRequestLine()}"
            ResponseHandler<String> handler = new RestResponse(command:this)
            return client.execute(request, handler)
        }
        finally {
            client.close()
        }
    }
}
