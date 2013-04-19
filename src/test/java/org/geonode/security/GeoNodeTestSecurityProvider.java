package org.geonode.security;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class GeoNodeTestSecurityProvider extends GeoNodeSecurityProvider {

    @Override
    protected GeoNodeSecurityClient configuredClient(String baseUrl) {
        return new MockSecurityClient();
    }



}
