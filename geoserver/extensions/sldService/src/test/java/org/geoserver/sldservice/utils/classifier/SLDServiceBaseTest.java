/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Before;

public abstract class SLDServiceBaseTest extends GeoServerSystemTestSupport {

	protected Map<String, Object> attributes = new HashMap<String, Object>();
	protected Object responseEntity;
	protected ResourcePool resourcePool;
	protected FeatureTypeInfoImpl testFeatureTypeInfo;

	protected SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
	protected StyleBuilder styleBuilder = new StyleBuilder();
	protected SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());

	protected static final String FEATURETYPE_LAYER = "featuretype_layer";

	protected static final String COVERAGE_LAYER = "coverage_layer";

    protected static Catalog catalog;
    protected static XpathEngine xp;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        //addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        testData.addWorkspace(testData.WCS_PREFIX, testData.WCS_URI, catalog);
        testData.addDefaultRasterLayer(testData.TASMANIA_DEM, catalog);
        testData.addDefaultRasterLayer(testData.TASMANIA_BM, catalog);
        testData.addDefaultRasterLayer(testData.ROTATED_CAD, catalog);
        testData.addDefaultRasterLayer(testData.WORLD, catalog);
        testData.addDefaultRasterLayer(testData.MULTIBAND,catalog);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();        
    }

    protected final void setUpUsers(Properties props) {
    }

    protected final void setUpLayerRoles(Properties properties) {
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    private Class<?> getGeometryType() {
		// TODO Auto-generated method stub
		return null;
	}

	protected Rule[] checkSLD(String resultXml) {
		sldParser.setInput(new StringReader(resultXml));
		StyledLayerDescriptor descriptor = sldParser.parseSLD();
		assertNotNull(descriptor);
		assertNotNull(descriptor.getStyledLayers());
		if(descriptor.getStyledLayers().length > 0) {
			StyledLayer layer = descriptor.getStyledLayers()[0];
			assertTrue(layer instanceof NamedLayer);
			NamedLayer namedLayer = (NamedLayer) layer;
			assertNotNull(namedLayer.getStyles());
			assertEquals(1, namedLayer.getStyles().length);
			Style style = namedLayer.getStyles()[0];
			assertNotNull(style.featureTypeStyles().toArray( new FeatureTypeStyle[0] ));
			assertEquals(1, style.featureTypeStyles().toArray( new FeatureTypeStyle[0] ).length);
			FeatureTypeStyle featureTypeStyle = style.featureTypeStyles().toArray( new FeatureTypeStyle[0] )[0];
			assertNotNull(featureTypeStyle.rules().toArray( new Rule[0] ));
			return featureTypeStyle.rules().toArray( new Rule[0] );
		} else {
			return null;
		}
	}

	protected abstract String getServiceUrl();
}
