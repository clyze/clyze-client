package doop.web.client
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
/**
 * The remote server response handler.
 *
 * @author: Kostas Saidis (saiko@di.uoa.gr)
 * Date: 12/2/2015
 */
class RestResponse implements ResponseHandler<String> {

    RestCommand command

    @Override
    String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity()
        int code = response.getStatusLine().getStatusCode()
        if (code == 200) {
            return command.onSuccess.call(entity)
        }
        else {
            String message = command.onError.call(code, entity)
            throw new ClientProtocolException(message)
        }
    }
}
