/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geonode.rest.batchdownload;

import org.geonode.process.control.DefaultProcessController;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestBaseController;

/**
 * Base controller for batch download process requests
 */
public abstract class AbstractBatchDownloadController extends RestBaseController {

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;
    protected final DefaultProcessController processController;

    public AbstractBatchDownloadController(Catalog catalog, DefaultProcessController processController) {
        super();
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.processController = processController;
    }

}
