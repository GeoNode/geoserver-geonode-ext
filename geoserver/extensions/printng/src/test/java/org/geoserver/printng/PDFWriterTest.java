package org.geoserver.printng;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.ParsedDocument;
import org.geoserver.printng.spi.PDFWriter;
import static testsupport.PrintTestSupport.assertPDF;
import org.junit.Test;

public class PDFWriterTest {

    @Test
    public void testWrite() throws IOException {
        String input = "<div>foobar</div>";
        ParsedDocument parser = ParsedDocument.parse(input);
        PDFWriter pdfWriter = new PDFWriter();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintSpec printSpec = new PrintSpec(parser);
        pdfWriter.write(printSpec, byteArrayOutputStream);
        assertPDF(byteArrayOutputStream);
    }
}
