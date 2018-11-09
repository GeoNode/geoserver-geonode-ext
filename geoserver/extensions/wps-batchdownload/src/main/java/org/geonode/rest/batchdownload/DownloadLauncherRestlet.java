package org.geonode.rest.batchdownload;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geonode.process.batchdownload.BatchDownloadFactory;
import org.geonode.process.batchdownload.LayerReference;
import org.geonode.process.batchdownload.MapMetadata;
import org.geonode.process.control.AsyncProcess;
import org.geonode.process.control.DefaultProcessController;
import org.geonode.process.control.ProcessController;
import org.geonode.process.control.ProcessStatus;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestBaseController;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.Processors;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * 
 * Expects a JSON object with the following structure:
 * <p>
 * <code>
 * <pre>
 *  {  map : { 
 *           title: "human readable title",
 *           abstract: "abstract information",
 *           author: "author name" 
 *           } 
 *    layers: 
 *        [
 *          {
 *              name:"&lt;layerName&gt;",
 *              service: "&lt;WFS|WCS&gt;,
 *              metadataURL: "&lt;csw request for the layer metadata?&gt;", 
 *              serviceURL:"&lt;serviceURL&gt;" //eg, "http://geonode.org/geoserver/wfs" 
 *          } ,...
 *        ]
 *  } 
 * </pre>
 * </code> or <code>
 * <pre>
 *  {  map : { 
 *           readme: "full content for the readme.tx file here...",
 *           } 
 *    layers: 
 *        [
 *          {
 *              name:"&lt;layerName&gt;",
 *              service: "&lt;WFS|WCS&gt;,
 *              metadataURL: "&lt;csw request for the layer metadata?&gt;", 
 *              serviceURL:"&lt;serviceURL&gt;" //eg, "http://geonode.org/geoserver/wfs" 
 *          } ,...
 *        ]
 *  } 
 * </pre>
 * </code>
 * 
 * 
 * Upon successful process launching returns a JSON object with the following structure: <code>
 * <pre>
 * {
 *   process:{
 *        id: &lt;processId&gt;
 *        status: &lt;WAITING|RUNNING|FINISHED|FAILED|CANCELLED&gt;
 *        statusMessage: "&lt;status message&gt;"
 *   }     
 * }
 * </pre>
 * </code>
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
@RestController
@ControllerAdvice
@RequestMapping(path = {
		RestBaseController.ROOT_PATH + "/process/batchDownload/launch"})
public class DownloadLauncherRestlet extends AbstractBatchDownloadController {

    private static Logger LOGGER = Logging.getLogger(DownloadLauncherRestlet.class);

    private static final Name PROCESS_NAME = new NameImpl("geonode", "BatchDownload");

    private final ProcessController controller;

    private GeoServerResourceLoader resources;
    
    @Autowired
    public DownloadLauncherRestlet(@Qualifier("catalog") Catalog catalog, @Qualifier("processController") ProcessController controller) {
    	super(catalog, (DefaultProcessController) controller);
        this.controller = controller;
        this.resources = catalog.getResourceLoader();
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity handle(Writer responseWriter) {
        LOGGER.finest("Parsing JSON request...");
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.write(responseWriter);

        LOGGER.finest("Launch request parsed, validating inputs and launching process...");
        final JSONObject responseData;
        try {
            responseData = execute(jsonRequest);
        } catch (ProcessException e) {
            final String message = "Process failed: " + e.getMessage();
            LOGGER.log(Level.INFO, message, e);
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(message);
        } catch (IllegalArgumentException e) {
            final String message = "Process can't be executed: " + e.getMessage();
            LOGGER.log(Level.INFO, message, e);
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(message);
        } catch (RuntimeException e) {
            final String message = "Unexpected exception: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(message);
        }

        final String jsonStr = responseData.toString(0);
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Process launched, response is " + jsonStr);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonStr);
    }

    private JSONObject execute(final JSONObject jsonRequest) throws ProcessException {

        final Map<String, Object> processInputs = convertProcessInputs(jsonRequest);

        final AsyncProcess process = (AsyncProcess) Processors.createProcess(PROCESS_NAME);
        if (process == null) {
            throw new IllegalStateException("Process factory not found for " + PROCESS_NAME);
        }

        final Long processId = controller.submitAsync(process, processInputs);

        JSONObject convertedOutputs = new JSONObject();
        convertedOutputs.element("id", processId.longValue());
        ProcessStatus status = controller.getStatus(processId);
        convertedOutputs.element("status", status.toString());
        float progress = controller.getProgress(processId);
        convertedOutputs.element("progress", progress);

        return convertedOutputs;
    }

    /**
     * Converts REST process inputs given as JSON objects to the actual {@link BatchDownloadFactory
     * process} inputs.
     * 
     * @param attributes
     * @return
     */
    private Map<String, Object> convertProcessInputs(final JSONObject request)
            throws ProcessException {

        Map<String, Object> processInputs;
        try {
            JSONObject map = request.getJSONObject("map");
            final MapMetadata mapDetails = convertMapMetadataParam(map);
            JSONArray layersParam = request.getJSONArray("layers");
            final List<LayerReference> layers = convertLayersParam(layersParam);

            processInputs = new HashMap<String, Object>();
            processInputs.put(BatchDownloadFactory.MAP_METADATA.key, mapDetails);
            processInputs.put(BatchDownloadFactory.LAYERS.key, layers);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return processInputs;
    }

    /**
     * Takes either a {@code "readme"} property as the complete contents for the README file, or the
     * following properties: {@code "title", "abstract", "author"}, whichever is present, in that
     * order of precedence.
     * 
     * @param obj
     *            the {@code "map"} json object
     * @return
     * @throws JSONException
     *             if a required json object property is not found
     */
    private MapMetadata convertMapMetadataParam(final JSONObject obj) throws JSONException {
        MapMetadata mmd;
        String readme = "";
        if (obj.containsKey("readme")) {
            readme = obj.getString("readme");
        }
        String title = obj.getString("title");
        if (title.length() == 0) {
            throw new IllegalArgumentException("Map name is empty");
        }
        String _abstract = obj.containsKey("abstract") ? obj.getString("abstract") : null;
        String author = obj.containsKey("abstract") ? obj.getString("author") : null;
        mmd = new MapMetadata(readme, title, _abstract, author);
        return mmd;
    }

    private List<LayerReference> convertLayersParam(final JSONArray obj) {
        if (obj == null || obj.size() == 0) {
            throw new IllegalArgumentException("no layers provided");
        }

        final int size = obj.size();
        List<LayerReference> layers = new ArrayList<LayerReference>(size);

        for (int layerN = 0; layerN < size; layerN++) {
            JSONObject layerParam = obj.getJSONObject(layerN);
            LayerReference layer = parseLayerReference(layerParam);
            layers.add(layer);
        }
        return layers;
    }

    private LayerReference parseLayerReference(final JSONObject layerParam) {
        final String layerName = layerParam.getString("name");
        final String service = layerParam.getString("service");
        final String serviceURL = layerParam.getString("serviceURL");

        final URL metadataURL;

        if (layerParam.containsKey("metadataURL")) {
            final String metadataURLParam = layerParam.getString("metadataURL");
            if (metadataURLParam.length() > 0) {
                try {
                    metadataURL = new URL(metadataURLParam);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("invalid format for metadataURL: '"
                            + metadataURLParam + "'");
                }
            } else {
                metadataURL = null;
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("metadataURL not provided for " + layerParam);
            }
            metadataURL = null;
        }

        LayerReference layer;
        if ("WFS".equals(service)) {
            FeatureSource<FeatureType, Feature> source = getFeatureSource(serviceURL, layerName);
            layer = new LayerReference(layerName, source);
        } else if ("WCS".equals(service)) {
            GridCoverage2DReader source = getCoverageReader(serviceURL, layerName);
            layer = new LayerReference(layerName, source);
        } else {
            throw new IllegalArgumentException("Invalid service name for layer '" + layerName
                    + "'. Expected one of WFS,WCS. Was '" + service + "'");
        }

        layer.setMetadataURL(metadataURL);

        return layer;
    }

    private GridCoverage2DReader getCoverageReader(final String serviceURL,
            final String layerName) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Ignoring serviceURL '" + serviceURL
                    + "'. Process only supports references to local resources by now.");
        }

        final CoverageInfo coverageInfo = catalog.getCoverageByName(layerName);
        if (coverageInfo == null) {
            throw new IllegalArgumentException("Coverage '" + layerName + "' does not exist");
        }
        GridCoverage2DReader reader;
        try {
            reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(
                    new NullProgressListener(), (Hints) null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieveing coverage '" + layerName + "': "
                    + e.getMessage(), e);
        }
        return reader;
    }

    @SuppressWarnings("unchecked")
    private FeatureSource<FeatureType, Feature> getFeatureSource(final String serviceURL,
            final String layerName) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Ignoring serviceURL '" + serviceURL
                    + "'. Process only supports references to local resources by now.");
        }

        final FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(layerName);
        if (typeInfo == null) {
            throw new IllegalArgumentException("Feature Type '" + layerName + "' does not exist");
        }
        FeatureSource<FeatureType, Feature> source;
        try {
            source = (FeatureSource<FeatureType, Feature>) typeInfo.getFeatureSource(
                    new NullProgressListener(), (Hints) null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieveing feature type '" + layerName + "': "
                    + e.getMessage(), e);
        }
        return source;
    }

}
