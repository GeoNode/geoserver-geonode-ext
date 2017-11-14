package org.geoserver.printng;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JOptionPane;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.PDFWriter;
import org.geoserver.printng.spi.ParsedDocument;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * These tests should render stuff and open it in a viewer for verification.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class AcceptanceTest {
    
    static List<File> output = new ArrayList<File>();

    @AfterClass
    public static void cleanup() {
        if (Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(null, "When done reviewing, press OK to cleanup", "Done", JOptionPane.PLAIN_MESSAGE);
        }
        for (File f: output) {
            if (!f.delete()) {
                throw new RuntimeException("Error deleting file: " + f.getPath());
            }
        }
    }

    @Test
    public void testMapRenderPNG() throws Exception {
        show(render("basic.html", new ImageWriter("png")));
    }

    @Test
    public void testMapRenderPDF() throws Exception {
        show(render("basic.html", new PDFWriter()));
    }

    private File render(String name, PrintngWriter writer) throws Exception {
        String map = getMap(name);
        ParsedDocument doc = ParsedDocument.parse(map);
        File tmp = File.createTempFile("render", ".png");
        output.add(tmp);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(tmp);
            writer.write(new PrintSpec(doc), fout);
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
        return tmp;
    }

    private String getMap(String name) {
        return new Scanner(getClass().getResourceAsStream("data/" + name)).useDelimiter("\\Z").next();
    }

    private void show(File map) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(map);
        } else {
            System.err.println("Desktop viewing of outputs not supported");
        }
    }
}
