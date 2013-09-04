package org.geonode.wfs;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;

import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A WFS transaction plugin that sets some useful transaction metadata obtained from the geonode
 * authentication credentials onto the transaction's "extended properties".
 * <p>
 * For instance, in geonode we have the full user name and email of the user performing the WFS
 * transaction, exposed as properties of the authentication object {@link GeoServerUser}. Whenever a
 * WFS transaction is about to be initiated, the properties {@code "fullname"} and {@code "email"}
 * will be extracted from the auth credentials and set as properties of {@link TransactionType}'s
 * extended properties map for lower level layers such as a geogit datastore to make use of them.
 */
public class GeonodeWFSCredentialsPlugin implements TransactionPlugin {

    private static final Logger LOGGER = Logging.getLogger(GeonodeWFSCredentialsPlugin.class);

    public TransactionType beforeTransaction(TransactionType request) throws WFSException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            LOGGER.fine("No authentication object found, can't determine geonode user name and email");
            return request;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof GeoServerUser)) {
            LOGGER.info("Auth object is not of type GeoServerUser, is geonode authentication present?");
            return request;
        }

        // pass along any user property
        final GeoServerUser gsUser = (GeoServerUser) principal;
        final Properties geonodeUserProperties = gsUser.getProperties();

        final String fullname = geonodeUserProperties.getProperty("fullname");
        final String email = geonodeUserProperties.getProperty("email");

        @SuppressWarnings("rawtypes")
        Map extendedTxProperties = request.getExtendedProperties();
        if (extendedTxProperties == null) {
            LOGGER.warning("WFS transaction extended properties is absent");
            return request;
        }

        if (fullname == null) {
            LOGGER.fine("current user's full name not available from geonode credentials");
        }
        if (email == null) {
            LOGGER.fine("current user's email not available from geonode credentials");
        }
        extendedTxProperties.put("fullname", fullname);
        extendedTxProperties.put("email", email);

        return request;
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        // do nothing
    }

    public void beforeCommit(TransactionType request) throws WFSException {
        // do nothing
    }

    public void afterTransaction(TransactionType request, TransactionResponseType result,
            boolean committed) {
        // do nothing
    }

    public int getPriority() {
        return 0;
    }

}
