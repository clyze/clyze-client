package org.clyze.client.web;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import java.net.URI;

/**
 * This class implements a DELETE method with a body (which is not supported
 * by the http-client due to the HTTP spec).
 *
 * Taken from https://github.com/android-async-http/android-async-http/issues/290
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";
    public String getMethod() { return METHOD_NAME; }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }
    public HttpDeleteWithBody(final URI uri) {
        super();
        setURI(uri);
    }
    public HttpDeleteWithBody() { super(); }
}
