package org.geonode.web.security;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geonode.security.GeoNodeAuthProviderConfig;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

public class GeoNodeAuthProviderPanelTest extends GeoServerWicketTestSupport {

    private static FormTestPage createProviderSource(GeoNodeAuthProviderConfig config) {
        return new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = 1L;
            public Component buildComponent(String id) {
                return new FormComponentTestingPanel(id, 
                        new GeoNodeAuthProviderPanel("formComponentPanel",
                            new Model<GeoNodeAuthProviderConfig>(config)));
            }});
        /*return new TestPanelSource() {
            public Panel getTestPanel(String id) {
                GeoNodeAuthProviderConfig config = new GeoNodeAuthProviderConfig();
                return new FormComponentTestingPanel(id, 
                    new GeoNodeAuthProviderPanel("formComponentPanel",
                        new Model<GeoNodeAuthProviderConfig>(config)));
            }
        };*/
    }
    
    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testVisitPanel() {
        GeoNodeAuthProviderConfig config = new GeoNodeAuthProviderConfig();
        login();
        tester.startPage(createProviderSource(config));
        tester.assertComponent("form:panel:formComponentPanel:baseUrl", TextField.class);
    }
}
