package org.geoserver.printng;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.ParsedDocument;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import testsupport.HTTPD;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class PrintUserAgentCallbackTest {

    static Server server;

    @Before
    public void clear() {
        server.requestHeaders.clear();
        server.uris.clear();
    }

    @BeforeClass
    public static void startServer() {
        server = new Server();
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Test
    public void testBase() throws IOException {
        PrintSpec spec =
                new PrintSpec(
                        ParsedDocument.parse(
                                String.format(
                                        "<img src='http://localhost:%s/foobar.png'>",
                                        server.getPort())));
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        // assertEquals(1, server.requestHeaders.size());
    }

    @Test
    public void testFileResource() throws IOException {
        String baseURL = String.format("file:///drive/root/doc.html");
        PrintSpec spec = new PrintSpec(ParsedDocument.parse("<img src='foobar.png'>", baseURL));
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        assertEquals(0, server.requestHeaders.size());
    }

    @Test
    public void testRelativeResource() throws IOException {
        String baseURL = String.format("http://localhost:%s/root/doc.html", server.getPort());
        PrintSpec spec = new PrintSpec(ParsedDocument.parse("<img src='foobar.png'>", baseURL));
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        // assertEquals(1, server.requestHeaders.size());
        // assertEquals("/root/foobar.png", server.uris.get(0));
    }

    @Test
    public void testCookies() throws IOException {
        PrintSpec spec =
                new PrintSpec(
                        ParsedDocument.parse(
                                String.format(
                                        "<img src='http://localhost:%s/foobar.png'>",
                                        server.getPort())));
        spec.addCookie("localhost", "foo", "bar");
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        // assertEquals(1, server.requestHeaders.size());
        // Properties props = server.requestHeaders.get(0);
        // assertEquals("foo=bar", props.getProperty("cookie"));
    }

    @Test
    public void testCreds() throws IOException {
        PrintSpec spec =
                new PrintSpec(
                        ParsedDocument.parse(
                                String.format(
                                        "<img src='http://localhost:%s/foobar.png'>",
                                        server.getPort())));
        spec.addCredentials("localhost", "foo", "bar");
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        assertEquals(1, server.requestHeaders.size());
        Properties props = server.requestHeaders.get(0);
        // assertEquals("Basic Zm9vOmJhcg==", props.get("authorization"));
    }

    static class Server extends HTTPD {

        List<String> uris = new ArrayList<String>();
        List<Properties> requestHeaders = new ArrayList<Properties>();

        @Override
        protected void serve(String uri, String method, Properties header, Properties parms) {
            uris.add(uri);
            requestHeaders.add(header);
            sendResponse("404", "text/plan", new Properties(), "ERROR");
        }
    }
}
