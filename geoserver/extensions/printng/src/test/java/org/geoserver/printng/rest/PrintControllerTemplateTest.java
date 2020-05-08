package org.geoserver.printng.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import org.apache.commons.io.IOUtils;
import org.geoserver.printng.GeoserverSupport;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class PrintControllerTemplateTest extends GeoServerSystemTestSupport {

    @Test
    public void testHandleTemplatePost() throws Exception {

        MockHttpServletResponse response =
                postAsServletResponse(
                        "/rest/printng/freemarker/foo",
                        "<div>foobar</div>",
                        MediaType.TEXT_HTML_VALUE);

        assertEquals("Invalid response", HttpStatus.CREATED.value(), response.getStatus());

        File directory = GeoserverSupport.getPrintngTemplateDirectory();
        File template = new File(directory, "foo.ftl");
        assertTrue("template wasn't created", template.exists());
        String contents = IOUtils.toString(new FileReader(template));
        /* assertEquals("Invalid template contents",
        "<?xml version=\"1.0\"?>\n<html><body><div>foobar</div></body></html>", contents); */
    }
}
