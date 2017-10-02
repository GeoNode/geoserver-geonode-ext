package org.geonode.rest.batchdownload;

import org.geonode.process.control.DefaultProcessController;
import org.geonode.process.control.ProcessController;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kills an ongoing batch download process.
 * <p>
 * Input: HTTP GET request to {@code <restlet end point>/<process id>}. For example:
 * {@code http://localhost:8080/geoserver/rest/process/batchdownload/kill/1001}.
 * </p>
 * <p>
 * Output: no content. Response status will be {@code 200 (OK)} if the process were running and has
 * been killed, or {@code 204 (SUCCESS NO CONTENT)} if the process has already finished at the time
 * the kill signal was sent. {@code 404 NOT FOUND} will be returned if the process didn't exist.
 * </p>
 */
@RestController
@ControllerAdvice
@RequestMapping(path = {
		RestBaseController.ROOT_PATH + "/process/batchDownload/kill"})
public class DownloadKillerRestlet extends AbstractBatchDownloadController {

	private final ProcessController controller;

    private GeoServerResourceLoader resources;
    
    @Autowired
    public DownloadKillerRestlet(@Qualifier("catalog") Catalog catalog, @Qualifier("processController") ProcessController controller) {
    	super(catalog, (DefaultProcessController) controller);
        this.controller = controller;
        this.resources = catalog.getResourceLoader();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity handle(@PathVariable(required = true) Long processId) {
        boolean killed;
        try {
            killed = controller.kill(processId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }

        if (killed) {
        	return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
        	return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
    }
}
