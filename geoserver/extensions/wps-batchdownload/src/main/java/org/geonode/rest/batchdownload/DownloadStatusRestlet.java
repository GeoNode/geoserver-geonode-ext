package org.geonode.rest.batchdownload;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geonode.process.control.DefaultProcessController;
import org.geonode.process.control.ProcessController;
import org.geonode.process.control.ProcessStatus;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestBaseController;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.sf.json.JSONObject;

/**
 * Returns the status code and progress percentage of a launched process.
 * <p>
 * Input: HTTP GET request to {@code <restlet end point>/<process id>}. For example:
 * {@code http://localhost:8080/geoserver/rest/process/batchdownload/status/1001}
 * </p>
 * <p>
 * Output: JSON object with the following structure:
 * 
 * <pre>
 * <code>
 * {
 *   process: {
 *     id: &lt;processId&gt;,
 *     status: "&lt;WAITING|RUNNING|FINISHED|FAILED&gt;",
 *     progress: &lt;percentage (0f - 100f)&gt;
 *   }
 * }
 * </code>
 * </pre>
 * 
 * </p>
 * 
 */
@RestController
@ControllerAdvice
@RequestMapping(path = {
		RestBaseController.ROOT_PATH + "/process/batchDownload/status"})
public class DownloadStatusRestlet extends AbstractBatchDownloadController {

    private static final Logger LOGGER = Logging.getLogger(DownloadStatusRestlet.class);

    private final ProcessController controller;

    private GeoServerResourceLoader resources;
    
    @Autowired
    public DownloadStatusRestlet(@Qualifier("catalog") Catalog catalog, @Qualifier("processController") ProcessController controller) {
    	super(catalog, (DefaultProcessController) controller);
        this.controller = controller;
        this.resources = catalog.getResourceLoader();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity handle(@PathVariable(required = true) Long processId) {
        ProcessStatus status;
        float progress;
        String errorMessage = null;
        try {
            status = controller.getStatus(processId);
            progress = controller.getProgress(processId);
            if (status == ProcessStatus.FAILED) {
                Throwable error = controller.getReasonForFailure(processId);
                if (error != null) {
                    errorMessage = error.getMessage();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        error.printStackTrace(new PrintStream(out));
                        String stackTrace = out.toString();
                        errorMessage += "\n" + stackTrace;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
        	return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }

        final JSONObject responseData = new JSONObject();
        final JSONObject processData = new JSONObject();
        processData.put("id", processId);
        processData.put("status", status.toString());
        processData.put("progress", progress);
        processData.put("reasonForFailure", errorMessage);
        responseData.put("process", processData);

        final String jsonStr = responseData.toString(0);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonStr);
    }
}
