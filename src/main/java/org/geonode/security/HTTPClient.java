package org.geonode.security;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.springframework.util.Assert;

/**
 * A reentrant HTTP client used to send authentication requests to GeoNode
 * 
 * @author groldan
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
public class HTTPClient {
    
	private static final Logger LOGGER = Logging.getLogger(HTTPClient.class);
	private int maxConnectionsPerHost;
	private int connectionTimeout;
	private int readTimeout;

    /**
     * 
     * @param maxConnectionsPerHost
     * @param connectionTimeout
     * @param readTimeout
     */
    public HTTPClient(final int maxConnectionsPerHost, final int connectionTimeout,
            final int readTimeout) {

        Assert.isTrue(maxConnectionsPerHost > 0,
                "maxConnectionsPerHost shall be a positive integer");
        Assert.isTrue(connectionTimeout >= 0,
                "connectionTimeout shall be a positive integer or zero");
        Assert.isTrue(readTimeout >= 0, "readTimeout shall be a positive integer or zero");

        this.maxConnectionsPerHost=maxConnectionsPerHost;
        this.connectionTimeout=connectionTimeout;
        this.readTimeout=readTimeout;

        
    }

    /**
     * Sends an HTTP GET request to the given {@code url} with the provided (possibly empty or null)
     * request headers, and returns the response content as a string.
     * 
     * @param url
     * @param requestHeaders
     * @return
     * @throws IOException
     */
    public String sendGET(final String url, final String[] requestHeaders) throws IOException {
    	Assert.notNull(url);
        GetMethod get = new GetMethod(url);
        get.setFollowRedirects(true);
       
        // good for subdomains
        get.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        final int numHeaders = requestHeaders == null ? 0 : requestHeaders.length / 2;
        for (int i = 0; i < numHeaders; i++) {
            String headerName = requestHeaders[2 * i];
            String headerValue = requestHeaders[1 + 2 * i];
            get.setRequestHeader(headerName, headerValue);
        }

        final int status;
        final String responseBodyAsString;

        try {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("HTTPClient::sendGET: Calling GeoNode with URL:" +url+" and requestHeaders "+Utilities.deepToString(get.getRequestHeaders()));
            }

            HttpConnectionManager manager= new SimpleHttpConnectionManager();
            manager.getParams().setConnectionTimeout(connectionTimeout);
            manager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
            manager.getParams().setSoTimeout(readTimeout);
			final HttpClient client= new HttpClient(manager);
            status = client.executeMethod(get);
            
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("HTTPClient::sendGET: Calling GeoNode with URL:" +url+" --> status "+status);
            }            
            if (status != 200) {
                throw new IOException("HTTPClient::sendGET: GeoNode communication failed, status report is: " + status
                        + ", " + get.getStatusText()
                    + ", " + url);
            }
            
            // releaseConnection will close the stream
            responseBodyAsString = IOUtils.toString(get.getResponseBodyAsStream());
            
        } finally {
            get.releaseConnection();
        }

        return responseBodyAsString;
    }
    
    
}
