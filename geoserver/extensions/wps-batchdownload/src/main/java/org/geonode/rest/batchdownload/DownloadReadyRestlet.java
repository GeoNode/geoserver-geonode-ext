package org.geonode.rest.batchdownload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geonode.process.batchdownload.BatchDownloadFactory;
import org.geonode.process.control.DefaultProcessController;
import org.geonode.process.control.ProcessController;
import org.geonode.process.storage.Resource;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves out the resulting zip file for a finished batch download process.
 * <p>
 * Input: HTTP GET request to {@code <restlet end point>/<process id>}. For example:
 * {@code http://localhost:8080/geoserver/rest/process/batchdownload/download/1001}
 * </p>
 * <p>
 * Output: ZIP file
 * </p>
 * 
 */
@RestController
@ControllerAdvice
@RequestMapping(path = {
		RestBaseController.ROOT_PATH + "/process/batchDownload/download"})
public class DownloadReadyRestlet extends AbstractBatchDownloadController {

	private final ProcessController controller;

    private GeoServerResourceLoader resources;
    
    @Autowired
    public DownloadReadyRestlet(@Qualifier("catalog") Catalog catalog, @Qualifier("processController") ProcessController controller) {
    	super(catalog, (DefaultProcessController) controller);
        this.controller = controller;
        this.resources = catalog.getResourceLoader();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity handle(@PathVariable(required = true) Long processId) {
        Map<String, Object> result;
        try {
            result = controller.getResult(processId);
        } catch (IllegalArgumentException e) {
        	return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }

        final Resource zipRes = (Resource) result.get(BatchDownloadFactory.RESULT_ZIP.key);

        final InputStream zip;
        try {
            zip = zipRes.getInputStream();
        } catch (IOException e) {
        	return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        String downloadName = null;
        try {
            File file = zipRes.getFile();
            downloadName = file.getName();
            long contentLength = file.length();
            headers.setContentLength(contentLength);
        } catch (Exception e) {
            // no worries, may the resource be not referencing a file in the filesystem but some
            // other kind of resource
        }
        if (downloadName != null) {
            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", downloadName));
        }
        
        try {
	        return new ResponseEntity<byte[]>(IOUtils.toByteArray(zip), headers, HttpStatus.CREATED);
        } catch (IOException e) {
        	return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
		} finally {
            try {
				zip.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}
