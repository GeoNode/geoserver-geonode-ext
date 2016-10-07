package org.geonode.rest.batchdownload;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.RASTER_LAYER;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.RASTER_LAYER_NAME;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.RESTLET_BASE_PATH;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.VECTOR_LAYER;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.VECTOR_LAYER_NAME;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.geonode.GeoNodeTestSupport;
import org.geonode.process.batchdownload.BatchDownloadFactory;
import org.geonode.process.batchdownload.LayerReference;
import org.geonode.process.batchdownload.MapMetadata;
import org.geonode.process.control.AsyncProcess;
import org.geonode.process.control.ProcessController;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.springframework.mock.web.MockHttpServletResponse;

public class DownloadReadyRestletTest extends GeoNodeTestSupport {

    private static final String RESTLET_PATH = RESTLET_BASE_PATH + "/download";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
        //dataDirectory.addWcs10Coverages();
    }
    
    public void testHTTPMethod() throws Exception {
        MockHttpServletResponse r = postAsServletResponse(RESTLET_PATH, "");
        assertEquals(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, Status.valueOf(r.getStatus()));
    }

    public void testInvalidProcessId() throws Exception {
        String request = RESTLET_PATH + "/notAProcessId";
        MockHttpServletResponse r = getAsServletResponse(request);
        assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, Status.valueOf(r.getStatus()));
    }

    public void testNonExistentProcess() throws Exception {
        String request = RESTLET_PATH + "/10000";
        MockHttpServletResponse r = getAsServletResponse(request);
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, Status.valueOf(r.getStatus()));
    }

    public void testDownload() throws Exception {

        final Long processId = issueProcessAndWaitForTermination();

        // zip extension is not required but should be handled
        final String request = RESTLET_PATH + "/" + processId.longValue() + ".zip";

        final MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(Status.SUCCESS_OK, Status.valueOf(response.getStatus()));
        assertEquals(MediaType.APPLICATION_ZIP, MediaType.valueOf(response.getContentType()));
        assertEquals("attachment; filename=\"test map.zip\"", response.getHeader("Content-Disposition"));

        final ByteArrayInputStream responseStream = getBinaryInputStream(response);
        final ZipInputStream zipIn = new ZipInputStream(responseStream);

        Set<String> expectedFiles = new HashSet<String>();
        expectedFiles.add("README.txt");
        expectedFiles.add(VECTOR_LAYER.getLocalPart() + ".shp");
        expectedFiles.add(VECTOR_LAYER.getLocalPart() + ".cst");
        expectedFiles.add(VECTOR_LAYER.getLocalPart() + ".prj");
        expectedFiles.add(VECTOR_LAYER.getLocalPart() + ".dbf");
        expectedFiles.add(VECTOR_LAYER.getLocalPart() + ".shx");
        // TODO: change this expectation once we normalize the raster file name
        expectedFiles.add(RASTER_LAYER.getPrefix() + ":" + RASTER_LAYER.getLocalPart() + ".tiff");

        Set<String> archivedFiles = new HashSet<String>();

        ZipEntry nextEntry;
        while ((nextEntry = zipIn.getNextEntry()) != null) {
            archivedFiles.add(nextEntry.getName());
        }

        assertEquals(expectedFiles, archivedFiles);
    }

    public void testCoverageContents() throws Exception {
        final Long processId = issueProcessAndWaitForTermination();

        final String request = RESTLET_PATH + "/" + processId.longValue();

        final MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(Status.SUCCESS_OK, Status.valueOf(response.getStatus()));
        assertEquals(MediaType.APPLICATION_ZIP, MediaType.valueOf(response.getContentType()));

        final ByteArrayInputStream responseStream = getBinaryInputStream(response);
        File dataDirectoryRoot = super.getTestData().getDataDirectoryRoot();
        File file = new File(dataDirectoryRoot, "testCoverageContents.zip");
        super.getTestData().copyTo(responseStream, file.getName());

        ZipFile zipFile = new ZipFile(file);
        try {
            // TODO: change this expectation once we normalize the raster file name
            String rasterName = RASTER_LAYER.getPrefix() + ":" + RASTER_LAYER.getLocalPart()
                    + ".tiff";
            ZipEntry nextEntry = zipFile.getEntry(rasterName);
            assertNotNull(nextEntry);
            InputStream coverageInputStream = zipFile.getInputStream(nextEntry);
            // Use a file, geotiffreader might not work well reading out of a plain input stream
            File covFile = new File(file.getParentFile(), "coverage.tiff");
            IOUtils.copy(coverageInputStream, covFile);
            GeoTiffReader geoTiffReader = new GeoTiffReader(covFile);
            GridCoverage2D coverage = geoTiffReader.read(null);
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
            assertEquals(CRS.decode("EPSG:4326", true), crs);
        } finally {
            zipFile.close();
        }
    }

    private Long issueProcessAndWaitForTermination() throws Exception {
        Map<String, Object> processInputs = new HashMap<String, Object>();
        MapMetadata mapMetadata = new MapMetadata("readme", "test map", "sample abstract info", "groldan");
        List<LayerReference> layers = new ArrayList<LayerReference>(1);
        layers.add(vectorLayer());
        layers.add(rasterLayer());
        processInputs.put(BatchDownloadFactory.MAP_METADATA.key, mapMetadata);
        processInputs.put(BatchDownloadFactory.LAYERS.key, layers);

        ProcessController controller = (ProcessController) GeoServerExtensions
                .bean("processController");

        AsyncProcess process = new BatchDownloadFactory().create();
        final Long processId = controller.submitAsync(process, processInputs);
        // wait for process termination
        while (!controller.isDone(processId)) {
            Thread.sleep(100);
        }
        return processId;
    }

    private LayerReference rasterLayer() throws IOException {
        Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(RASTER_LAYER_NAME);
        GridCoverage2DReader reader;
        reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);

        LayerReference layerReference = new LayerReference(RASTER_LAYER_NAME, reader);
        return layerReference;
    }

    private LayerReference vectorLayer() throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(VECTOR_LAYER_NAME);
        FeatureSource<? extends FeatureType, ? extends Feature> source;
        source = typeInfo.getFeatureSource(null, null);

        LayerReference layerReference = new LayerReference(VECTOR_LAYER_NAME, source);
        return layerReference;
    }
}
