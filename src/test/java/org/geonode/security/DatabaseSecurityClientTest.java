/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geonode.security;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.easymock.classextension.EasyMock;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @TODO mock database response and test
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class DatabaseSecurityClientTest {
    
    private HTTPClient mockHttpClient;

    private DatabaseSecurityClient client;

    @Before
    public void setUp() throws Exception {
        mockHttpClient = EasyMock.createNiceMock(HTTPClient.class);
        client = new DatabaseSecurityClient(null, "http://localhost:8000/", mockHttpClient);
    }

    @Test
    public void testAuthenticateUserPassword() throws Exception {
        String username = "aang";
        String password = "katara";
        final String[] requestHeaders = { "Authorization",
                "Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes())) };

        final String response = "{\"superuser\": true, \"user\": \"aang\", \"geoserver\": \"false\"}";

        EasyMock.expect(
                mockHttpClient.sendGET(EasyMock.eq("http://localhost:8000/layers/resolve_user"),
                        EasyMock.aryEq(requestHeaders))).andReturn(response);
        EasyMock.replay(mockHttpClient);

        Authentication authentication = client.authenticateUserPwd(username, password);
        EasyMock.verify(mockHttpClient);

        assertNotNull(authentication);
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
        assertTrue(authentication.isAuthenticated());
        assertEquals("aang", ((UserDetails)authentication.getPrincipal()).getUsername() );

        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.addAll(authentication.getAuthorities());
        assertTrue(authorities.contains(GeoServerRole.ADMIN_ROLE));
        assertTrue(authorities.contains(GeoServerRole.AUTHENTICATED_ROLE));
    }
}