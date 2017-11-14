package org.geoserver.printng;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 * Home of otherwise homeless static methods.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class PrintSupport {
    
    private PrintSupport() {}

    public static File getGlobalCacheDir() {
        return new File(System.getProperty("java.io.tmpdir"), "printng-cache");
    }
    
    /**
     * Scale an image to a specified width/height.
     * @param im The image to scale
     * @param width The expected width
     * @param height The expected height
     * @param exact If true, ensure the output matches, otherwise use an aspect
     * @return scaled image
     */
    public static BufferedImage niceImage(BufferedImage im, int width, int height, boolean exact) {
        int ts = Math.max(width, height);
        double aspect = (double) im.getWidth() / (double) im.getHeight();
        int sw = ts;
        int sh = ts;

        if (aspect < 1) {
            sw *= aspect;
        } else if (aspect > 1) {
            sh /= aspect;
        }
        double scale = (double) Math.max(sw, sh) / Math.max(im.getWidth(), im.getHeight());
        BufferedImage scaled;
        if (exact) {
            if (scale * im.getWidth() < width) {
                scale = (double) width / im.getWidth();
            }
            if (scale * im.getHeight() < height) {
                scale = (double) height / im.getHeight();
            }
            scaled = new BufferedImage(width, height, im.getType());
        } else {
            scaled = new BufferedImage(sw, sh, im.getType());
        }
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform trans = new AffineTransform();
        trans.scale(scale, scale);

        g2.drawRenderedImage(im, trans);
        return scaled;
    }
    
    public static void write(Document dom, OutputStream out, boolean indent) {
        Transformer trans;
        try {
            trans = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        try {
            trans.setOutputProperty(OutputKeys.METHOD, "html");
            if (indent) {
                trans.setOutputProperty(OutputKeys.INDENT, "4");
            }
            trans.transform(new DOMSource(dom), new StreamResult(out));
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String toString(Document dom) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(dom, baos, true);
        return baos.toString();
    }
}
