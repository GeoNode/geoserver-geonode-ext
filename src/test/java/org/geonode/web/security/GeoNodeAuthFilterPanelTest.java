package org.geonode.web.security;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.geonode.security.GeoNodeAuthFilterConfig;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

public class GeoNodeAuthFilterPanelTest extends GeoServerWicketTestSupport {
    
    private static FormTestPage createProviderSource(final GeoNodeAuthFilterConfig config) {
        return new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = 1L;
            public Component buildComponent(String id) {
                return new FormComponentTestingPanel(id, 
                    new GeoNodeAuthFilterPanel("formComponentPanel",
                        new Model<GeoNodeAuthFilterConfig>(config)));
        }});
    }
    
    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testVisitPanel() {
        GeoNodeAuthFilterConfig config = new GeoNodeAuthFilterConfig();
        login();
        tester.startPage(createProviderSource(config));
        tester.assertComponent("form:panel:formComponentPanel", GeoNodeAuthFilterPanel.class);
    }
}