package org.geonode.security;

import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;

public class GeoNodeAnonymousSecurityProvider extends GeoServerSecurityProvider {
    @Override
    public Class<GeoNodeAnonymousProcessingFilter> getFilterClass() {
        return GeoNodeAnonymousProcessingFilter.class;
    }
    
    @Override
    public GeoNodeAnonymousProcessingFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoNodeAnonymousProcessingFilter();
    }
}
