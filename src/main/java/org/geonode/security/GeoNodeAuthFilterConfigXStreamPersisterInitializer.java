package org.geonode.security;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

import com.thoughtworks.xstream.XStream;

/**
 * Configure XStreamPersisters for WFS
 * 
 * @author Sampo Savolainen (Spatineo)
 */
public class GeoNodeAuthFilterConfigXStreamPersisterInitializer implements
		XStreamPersisterInitializer {

	@Override
	public void init(XStreamPersister persister) {
		XStream xs = persister.getXStream();
        xs.allowTypes(
                new Class[] { GeoNodeAuthProviderConfig.class, GeoNodeAuthFilterConfig.class, GeoNodeAnonymousAuthFilterConfig.class });
	}

}
