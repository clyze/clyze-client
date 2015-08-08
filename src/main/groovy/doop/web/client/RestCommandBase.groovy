package doop.web.client

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

/**
 * Created by saiko on 7/8/2015.
 */
class RestCommandBase<T> implements ResponseHandler<T> {

    public static final String BASE_PATH = "/jdoop/web/api/v1/"
    public static final String HEADER_TOKEN = "x-doop-token"

    public static final Closure<String> DEFAULT_ERROR = { int statusCode, HttpEntity entity ->
        String message = entity ? EntityUtils.toString(entity) : "No message"
        return "Error $statusCode: $message"
    }

    protected Log logger = LogFactory.getLog(getClass())

    /** The Restful endpoint of the command (suffix to the {@code BASE_PATH}.*/
    String endPoint

    /** Indication whether the command requires authentication */
    boolean authenticationRequired = true

    /** A closure that creates HttpUriRequest objects. It accepts a single parameter, the url. The closure's delegate
     * is the command itself.
     */
    Closure<HttpUriRequest> requestBuilder

    /** A closure to be executed in case of success (status code == 200). It accepts an HttpEntity object and returns a
     * T. The closure's delegate is the command itself.
     */
    Closure<T> onSuccess

    /** A closure to be executed in case of error (status code != 200). It accepts the status code and the HttpEntity
     * object and return the error message.
     */
    Closure<String> onError = DEFAULT_ERROR

    /**
     * A closure to be executed for authenticating the user. It accepts three arguments: (a) the host and (b) the port
     * of the remote doop server as well as (c) the HttpUriRequest object. The closure's delegate is the command itself.
     */
    Closure<Void> authenticator

    HttpUriRequest buildRequest(String url) {
        if (! requestBuilder) throw new RuntimeException("No requestBuilder for the command")
        requestBuilder.delegate = this
        return requestBuilder.call(url)
    }

    protected String createURL(String host, int port) {
        return "http://${host}:${port}${BASE_PATH}${endPoint}"
    }

    protected CloseableHttpClient createHttpClient() {
        def hourInMillis = 1000 * 60 * 60
        RequestConfig config = RequestConfig.custom().setSocketTimeout(hourInMillis).build()
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build()
    }

    T execute(String host, int port) {

        String url = createURL(host, port)
        CloseableHttpClient client = createHttpClient()

        try {
            HttpUriRequest request = buildRequest(url)
            if (authenticationRequired) {
                if (! authenticator) throw new RuntimeException("No authenticator for the command")
                authenticator.delegate = this
                authenticator.call(host, port, request)
            }

            logger.debug "Executing request: ${request.getRequestLine()}"
            return client.execute(request, this)
        }
        finally {
            client.close()
        }
    }

    @Override
    T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity()
        int code = response.getStatusLine().getStatusCode()
        if (code == 200) {
            if (! onSuccess) throw new RuntimeException("No success handler for the command")
            onSuccess.delegate = this
            return onSuccess.call(entity)
        }
        else {
            String message = onError.call(code, entity)
            throw new ClientProtocolException(message)
        }
    }
}
