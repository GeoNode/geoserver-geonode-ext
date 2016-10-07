package org.geonode.security;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geonode.GeoNodeTestSupport;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SecuredAccessTest extends GeoNodeTestSupport {

    static MockSecurityClient client;

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    /**
     * This is a security test, we want the authorization backed to actually do
     * its work
     */
    @Override
    protected boolean isAuthorizationEnabled() {
        return true;
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext.xml");
        springContextLocations.add("classpath*:/applicationSecurityContext.xml");
        springContextLocations.add("classpath*:/testApplicationContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoNodeTestSecurityProvider geoNodeTestSecurityProvider = (GeoNodeTestSecurityProvider) applicationContext.getBean("geoNodeSecurityProvider");
        client = (MockSecurityClient) geoNodeTestSecurityProvider.getSecurityClient();
        
        Logging.getLogger("").setLevel(Level.ALL);
        Logging.getLogger("org.geonode.security").setLevel(Level.ALL);
    }

    /**
     * No authentication, we should get a challenge to authenticate
     */
    public void testNoAuth() throws Exception {
        client.setAnonymousRights(false, null, null);

        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        // In HIDE mode restricted access gets an OGC service error as if the layer does not exist
        assertEquals(200, resp.getStatus());
        Document doc = dom(new ByteArrayInputStream(resp.getContentAsByteArray()));
        assertNull(resp.getHeader("WWW-Authenticate"));
        assertXpathEvaluatesTo("0", "count(/wfs:FeatureCollection)", doc);
    }

    public void testAdminBasic() throws Exception {
        String username = "admin";
        String password = "geonode";
        client.addUserAuth(username, password, true, null, null);

        checkValidBasicAuth(username, password);
    }

    public void testAdminCookie() throws Exception {
        String username = "admin";
        String cookie = "geonode-auth-abcde";
        client.addCookieAuth(cookie, username, true, null, null);

        checkValidCookieAuth(cookie);
    }

    public void testUserBasicRead() throws Exception {
        String username = "joe";
        String password = "secret";
        client.addUserAuth(username, password, false,
                Collections.singletonList(getLayerId(MockData.BUILDINGS)), null);

        checkValidBasicAuth(username, password);
    }

    public void testUserBasicReadWrite() throws Exception {
        String username = "joe";
        String password = "secret";
        client.addUserAuth(username, password, false, null,
                Collections.singletonList(getLayerId(MockData.BUILDINGS)));

        checkValidBasicAuth(username, password);
    }

    public void testUserCookie() throws Exception {
        String username = "joe";
        String cookie = "geonode-auth-lameuser";
        client.addCookieAuth(cookie, username, false,
                Collections.singletonList(getLayerId(MockData.BUILDINGS)), null);

        checkValidCookieAuth(cookie);
    }

    void checkValidBasicAuth(String username, String password) throws Exception,
            ParserConfigurationException, SAXException, IOException, XpathException {
        MockHttpServletRequest request = createRequest("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        request.addHeader("Authorization",
                "Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes())));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(200, resp.getStatus());
        Document doc = dom(new ByteArrayInputStream(resp.getContentAsByteArray()));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }

    void checkValidCookieAuth(String cookie) throws Exception, ParserConfigurationException,
            SAXException, IOException, XpathException {
        MockHttpServletRequest request = createRequest("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        request.setCookies(new Cookie(GeoNodeCookieProcessingFilter.GEONODE_COOKIE_NAME, cookie));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(200, resp.getStatus());
        Document doc = dom(new ByteArrayInputStream(resp.getContentAsByteArray()));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }

    /**
     * Checks an anonymous user that has layer grants (which can happen in
     * GeoNode)
     *
     * @throws Exception
     */
    public void testAnonymousAuthWithGrants() throws Exception {
        client.setAnonymousRights(false, Collections.singletonList(getLayerId(MockData.BUILDINGS)),
                null);
        MockHttpServletRequest request = createRequest("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(200, resp.getStatus());
        Document doc = dom(new ByteArrayInputStream(resp.getContentAsByteArray()));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }
}
