package org.geonode.rest.batchdownload;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.geonode.rest.batchdownload.BatchDownloadTestData.RESTLET_BASE_PATH;

import java.util.Collections;
import java.util.Map;

import org.geonode.GeoNodeTestSupport;
import org.geonode.process.control.AsyncProcess;
import org.geonode.process.control.ProcessController;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.process.ProcessException;
import org.opengis.util.ProgressListener;
import org.restlet.data.Status;
import org.springframework.mock.web.MockHttpServletResponse;

public class DownloadKillerRestletTest extends GeoNodeTestSupport {

    private static final String RESTLET_PATH = RESTLET_BASE_PATH + "/kill";

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

    public void testKillRunningProcess() throws Exception {

        ProcessController controller = (ProcessController) GeoServerExtensions
                .bean("processController");

        final Long processId = issueProcess(10);

        assertFalse(controller.isDone(processId));

        final String request = RESTLET_PATH + "/" + processId.longValue();

        final MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(Status.SUCCESS_OK, Status.valueOf(response.getStatus()));
    }

    public void testKillFinishedProcess() throws Exception {

        ProcessController controller = (ProcessController) GeoServerExtensions
                .bean("processController");

        final Long processId = issueProcess(1);
        Thread.sleep(2000);

        assertTrue(controller.isDone(processId));

        final String request = RESTLET_PATH + "/" + processId.longValue();

        final MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(Status.SUCCESS_NO_CONTENT, Status.valueOf(response.getStatus()));
    }

    /**
     * Issues a fake process that finishes normally after {@code timeOutSeconds} seconds if not
     * killed
     * 
     * @param i
     * 
     * @return
     * @throws Exception
     */
    private Long issueProcess(final int timeOutSeconds) throws Exception {

        ProcessController controller = (ProcessController) GeoServerExtensions
                .bean("processController");

        AsyncProcess process = new AsyncProcess() {
            @Override
            protected Map<String, Object> executeInternal(final Map<String, Object> input,
                    final ProgressListener monitor) throws ProcessException {

                final long launchTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - launchTime < timeOutSeconds * 1000L) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new ProcessException(e);
                    }
                    if (monitor.isCanceled()) {
                        return null;
                    }
                }
                return Collections.emptyMap();
            }
        };
        Map<String, Object> processInputs = Collections.emptyMap();
        final Long processId = controller.submitAsync(process, processInputs);

        return processId;
    }
}
