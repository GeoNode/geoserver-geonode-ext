/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.geoserver.catalog.LayerInfo;
import org.junit.Test;
import org.w3c.dom.Document;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class LayerAttributesTest extends SLDServiceBaseTest {

	@Test
	public void testListAttributesForFeatureXml() throws Exception {
		LayerInfo l = catalog.getLayerByName( "cite:Buildings" );
        assertEquals( "Buildings", l.getDefaultStyle().getName() );
        Document dom = getAsDOM(ROOT_PATH + "/sldservice/cite:Buildings/"+getServiceUrl()+".xml", 200);
        // print(dom);
        assertEquals( "Attributes", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("cite:Buildings", "/Attributes/@layer", dom );
        assertXpathEvaluatesTo("FID", "/Attributes/Attribute[1]/name", dom );		
        assertXpathEvaluatesTo("String", "/Attributes/Attribute[1]/type", dom );		
	}

	@Test
	public void testListAttributesForFeatureJson() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:Buildings" );
        assertEquals( "Buildings", l.getDefaultStyle().getName() );
        JSONObject json = (JSONObject) getAsJSON(ROOT_PATH + "/sldservice/cite:Buildings/"+getServiceUrl()+".json");
        // print(json);
        JSONObject layerAttributes = (JSONObject)json.get("Attributes");
        String layer = (String) layerAttributes.get("@layer");
        assertEquals(layer, "cite:Buildings");
        JSONArray attributes = (JSONArray) layerAttributes.get("Attribute");
        assertEquals(attributes.toArray().length, 3);
        assertEquals(((JSONObject)attributes.get(0)).get("name"), "FID");
        assertEquals(((JSONObject)attributes.get(0)).get("type"), "String");
	}

	@Test
	public void testListAttributesForCoverageIsEmpty() throws Exception {
		LayerInfo l = catalog.getLayerByName("World");
        assertEquals( "raster", l.getDefaultStyle().getName() );
        Document dom = getAsDOM(ROOT_PATH + "/sldservice/wcs:World/"+getServiceUrl()+".xml", 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
		assertTrue(baos.toString().indexOf("<list/>")>0);
	}

	@Override
	protected String getServiceUrl() {
		return "attributes";
	}
}
