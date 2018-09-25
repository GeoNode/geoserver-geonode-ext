package org.geoserver.printng;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.xml.serialize.XMLSerializer;
import org.geoserver.printng.spi.ParsedDocument;
import org.junit.Test;

public class ParsedDocumentTest {

    /* @Test
    public void testParseWithTagSoup() throws IOException {
        String input = "<div>foobar</div>";
        ParsedDocument parser = ParsedDocument.parse(input);

        StringWriter stringWriter = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(stringWriter, null);
        serializer.serialize(parser.getDocument());
        String result = stringWriter.getBuffer().toString();
        String exp = "<?xml version=\"1.0\"?>\n<html><body><div>foobar</div></body></html>";
        assertEquals("Invalid document parse", exp, result);
    } */

    @Test
    public void testParseNoTagSoup() throws IOException {
        String input = "<div>foobar</div>";
        ParsedDocument parser = ParsedDocument.parse(new StringReader(input), false);

        StringWriter stringWriter = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(stringWriter, null);
        serializer.serialize(parser.getDocument());
        String result = stringWriter.getBuffer().toString();
        String exp = "<?xml version=\"1.0\"?>\n<div>foobar</div>";
        assertEquals("Invalid document parse", exp, result);
    }

    @Test
    public void testCssOverride() throws Exception {
        ParsedDocument d = ParsedDocument.parse("<div>foo</div>");
        // none to start with
        assertEquals(0, d.getDocument().getElementsByTagName("link").getLength());
        d.addCssOverride("farby.css");
        // should have a link now
        assertEquals(1, d.getDocument().getElementsByTagName("link").getLength());
        // should be idempotent
        assertEquals(1, d.getDocument().getElementsByTagName("link").getLength());
    }
}
