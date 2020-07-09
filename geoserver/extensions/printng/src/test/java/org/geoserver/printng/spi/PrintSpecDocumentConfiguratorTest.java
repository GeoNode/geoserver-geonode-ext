package org.geoserver.printng.spi;

import org.geoserver.printng.api.PrintSpec;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecDocumentConfiguratorTest {
    
    PrintSpec spec = new PrintSpec(null);
    
    @Test
    public void testConfigureNativeDimensions() throws Exception {
        ParsedDocument doc = ParsedDocument.parse("<div style='width:25;height:50'></div>");
        PrintSpecDocumentConfigurator.configure(spec, doc.getDocument());
        assertEquals(25, spec.getWidth());
        assertEquals(50, spec.getHeight());
        
        spec = new PrintSpec(null);
        doc = ParsedDocument.parse("<div style='width:75px;height:150px'></div>");
        PrintSpecDocumentConfigurator.configure(spec, doc.getDocument());
        assertEquals(75, spec.getWidth());
        assertEquals(150, spec.getHeight());
        
        spec = new PrintSpec(null);
        doc = ParsedDocument.parse("<body style='width:75px;height:150px'></body>");
        PrintSpecDocumentConfigurator.configure(spec, doc.getDocument());
        assertEquals(75, spec.getWidth());
        assertEquals(150, spec.getHeight());
        
        spec = new PrintSpec(null);
        doc = ParsedDocument.parse("<div></div><p>foo<p>bar<div></div>");
        PrintSpecDocumentConfigurator.configure(spec, doc.getDocument());
        assertTrue(! spec.isRenderDimensionSet());
    }
}
