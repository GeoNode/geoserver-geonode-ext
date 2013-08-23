/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geonode.security;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.impl.GeoServerUser;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * An {@link AuthenticationProvider} provider passing the username/password to GeoNode for
 * authentication
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeoNodeAuthenticationProvider extends GeoServerAuthenticationProvider {

    private GeoNodeSecurityClient client;

    public GeoNodeAuthenticationProvider(GeoNodeSecurityClient client) {
        this.client = client;
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request) throws AuthenticationException {
    	if (authentication instanceof UsernamePasswordAuthenticationToken) {
	        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
	        String username = token.getName();
	        String password = (String) token.getCredentials();

            // ignore this - let the other provider(s) handle things
            if (GeoServerUser.ROOT_USERNAME.equals(username) && GeoServerUser.DEFAULT_ADMIN_PASSWD.equals(username)) {
                return null;
            }
	
	        try {
	        	if (username == "" && password == null) {
	        		return client.authenticateAnonymous();
                } else {
                    // if an anonymous session cookie exists in the request but
                    // the user is logging in via the admin or other form mechanism,
                    // it's possible that the GeoNodeCookieProcessingFilter will
                    // 'overwrite' the credentials... it will check for this
                    Authentication auth = client.authenticateUserPwd(username, password);
                    if (auth.isAuthenticated()) {
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                    return auth;
                }
	        } catch (IOException e) {
	            throw new AuthenticationServiceException("Communication with GeoNode failed", e);
	        }
	    } else if (authentication instanceof GeoNodeSessionAuthToken) {
	    	try {
	    		return client.authenticateCookie((String) authentication.getCredentials());
	    	} catch (IOException e) {
	    		throw new AuthenticationServiceException("Communication with GeoNode failed", e);
	    	}
	    } else if (authentication instanceof AnonymousGeoNodeAuthenticationToken) {
	       try { 
	           return client.authenticateAnonymous();
	       } catch (IOException e) {
	           throw new AuthenticationServiceException("Communication with GeoNode failed", e);
	       }
	    } else {
	    	throw new IllegalArgumentException("GeoNodeAuthenticationProvider accepts only UsernamePasswordAuthenticationToken and GeoNodeSessionAuthToken; received " + authentication);
	    }
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        	    GeoNodeSessionAuthToken.class.isAssignableFrom(authentication)) ||
        	    AnonymousGeoNodeAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 
     * @param client
     */
    public void setClient(GeoNodeSecurityClient client) {
        this.client = client;
    }

}
