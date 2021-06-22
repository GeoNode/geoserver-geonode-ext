package org.geoserver.printng.spi;

import java.awt.Dimension;
import java.util.logging.Level;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintSpecConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Attempt to configure the native size of the image from the document. This
 * currently supports a style attribute containing width and height that is
 * specified on the document body element or the first child.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecDocumentConfigurator extends PrintSpecConfigurator<Document> {

    private PrintSpecDocumentConfigurator(Document dom) {
        super(dom);
    }

    public static PrintSpec configure(PrintSpec spec, Document dom) throws PrintSpecException {
        return new PrintSpecDocumentConfigurator(dom).configure(spec);
    }

    @Override
    protected void configureSpec(PrintSpec spec) {
        if (!spec.isRenderDimensionSet()) {
            Dimension nativeSize = null;
            NodeList els = source.getDocumentElement().getElementsByTagName("body");
            Element body = els.getLength() > 0 ? (Element) els.item(0) : null;
            nativeSize = extractSize(body);
            if (nativeSize == null && body != null) {
                Node child = body.getFirstChild();
                while (child != null) {
                    if (child instanceof Element) {
                        nativeSize = extractSize((Element) child);
                        if (nativeSize != null) {
                            break;
                        }
                    }
                    child = child.getNextSibling();
                }
            }
            if (nativeSize != null) {
                messages.log(Level.FINE, "configuring render size from document to {0}", nativeSize);
                spec.setWidth(nativeSize.width);
                spec.setHeight(nativeSize.height);
            }
        }
    }

    private Integer parseInt(String spec) {
        Integer parsed = null;
        try {
            parsed = new Integer(spec.replace("px", "").trim());
        } catch (NumberFormatException ex) {
            messages.log(Level.FINE, "Unable to parse document dimension from {0}", spec);
        }
        return parsed;
    }

    private Dimension extractSize(Element el) {
        Integer nativeWidth = null;
        Integer nativeHeight = null;
        if (el != null) {
            String style = el.getAttribute("style");
            String[] chunks = style.split(";");
            for (int i = 0; i < chunks.length; i++) {
                String[] parts = chunks[i].split(":");
                if (parts[0].trim().equalsIgnoreCase("width")) {
                    nativeWidth = parseInt(parts[1]);
                } else if (parts[0].trim().equalsIgnoreCase("height")) {
                    nativeHeight = parseInt(parts[1]);
                }
            }
        }
        Dimension dim = null;
        if (nativeWidth != null && nativeHeight != null) {
            dim = new Dimension(nativeWidth, nativeHeight);
        }
        return dim;
    }
}
