package org.clyze.client.web.http

//import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

//@CompileStatic
@Log4j
class HttpClientCommand<T> implements ResponseHandler<T> {

    public static final Closure<String> DEFAULT_ERROR = { int statusCode, HttpEntity entity ->
        String message = entity ? EntityUtils.toString(entity) : "No message"
        return "Error $statusCode: $message" as String
    }

    /** The http client life cycle manager */
    HttpClientLifeCycle httpClientLifeCycle
    
    /** A closure that creates HttpUriRequest objects. It accepts a single parameter, the url. The closure's delegate
     * is the command itself.
     */
    Closure<? extends HttpUriRequest> requestBuilder

    /** A closure to be executed in case of success (status code == 200). It accepts an HttpEntity object and returns a
     * T. The closure's delegate is the command itself.
     */
    Closure<T> onSuccess

    /** A closure to be executed in case of error (status code != 200). It accepts the status code and the HttpEntity
     * object and return the error message.
     */
    Closure<String> onError = DEFAULT_ERROR    

    static HttpClientCommand extend(Map<String, Object> attrsToOverride=[:], HttpClientCommand cmd) {    
        def attrs = ["httpClientLifeCycle", "requestBuilder", "onSuccess", "onError"]
        def map = extendMap(attrs, attrsToOverride, cmd)
        return new HttpClientCommand(map)
    }

    protected static Map extendMap(List<String> attrs, Map<String, Object> attrsToOverride, HttpClientCommand cmd) {
        attrs.collectEntries { String attr ->
            [(attr): attrsToOverride[attr] ?: cmd[attr]]
        }        
    }

    HttpUriRequest buildRequest(String host, int port) {
        if (! requestBuilder) throw new RuntimeException("Command is not configured correctly (no request builder)")
        requestBuilder.delegate = this
        return requestBuilder.call(host, port)
    }        

    T execute(String host, int port) {

        if (httpClientLifeCycle == null) {
            throw new RuntimeException("Command is not configured correctly (no lifecycle)")
        }
        
        CloseableHttpClient client = httpClientLifeCycle.createHttpClient()

        try {
            HttpUriRequest request = buildRequest(host, port)            
            log.debug "Executing request: ${request.getRequestLine()}"
            return client.execute(request, this) as T
        }
        finally {
            httpClientLifeCycle.closeHttpClient(client)
        }
    }

    @Override
    T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity()
        int code = response.getStatusLine().getStatusCode()
        if (code == 200) {            
            if (! onSuccess) throw new RuntimeException("Command is not configured correctly (no success handler)")            
            onSuccess.delegate = this
            return onSuccess.call(entity)
        }
        else {
            String message = onError.call(code, entity)
            throw new ClientProtocolException(message)
        }
    }
}
