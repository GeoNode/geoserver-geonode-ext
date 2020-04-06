package org.geoserver.printng;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.PDFWriter;
import org.geoserver.printng.spi.ParsedDocument;

/**
 * Use this as an interactive test driver.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLToPDF {

    public static void main(String[] args)
            throws Exception {
        Logger.getLogger("").addAppender(new ConsoleAppender());
        int ppd = 20;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        boolean img = argList.removeFirstOccurrence("-img");
        boolean loop = argList.removeFirstOccurrence("-loop");
//        boolean cache = argList.removeFirstOccurrence("-cache");
        if (argList.removeFirstOccurrence("-loghttp")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        }
        List<String> creds = null;
        List<String> cookie = null;
        String css = null;
        int idx = argList.indexOf("-auth");
        if (idx >= 0) {
            argList.remove(idx);
            creds = new ArrayList<String>(argList.subList(idx, idx + 3));
            argList.removeAll(creds);
        }
        idx = argList.indexOf("-cookie");
        if (idx >= 0) {
            argList.remove(idx);
            cookie = new ArrayList<String>(argList.subList(idx, idx + 3));
            argList.removeAll(cookie);
        }
        idx = argList.indexOf("-css");
        if (idx >= 0) {
            argList.remove(idx);
            css = argList.remove(idx);
            File cssFile = new File(css);
            if (cssFile.exists()) {
                css = cssFile.getAbsolutePath();
            }
        }
        
        File inputFile = new File(argList.pop());
        File outputFile;
        PrintngWriter writer;
        if (!argList.isEmpty()) {
            outputFile = new File(argList.pop());
        } else {
            String ext = img ? ".png" : ".pdf";
            outputFile = new File(inputFile.getName().replace(".html", ext));
        }
        while (true) {
            ParsedDocument document = ParsedDocument.parse(inputFile, true);
            PrintSpec printSpec = new PrintSpec(document);
            printSpec.setCacheDirRoot(new File("cache"));
            printSpec.setOutputWidth(512);
            printSpec.setOutputHeight(256);
            printSpec.setDotsPerPixel(ppd);
            printSpec.setCssOverride(css);
            if (creds != null) {
                printSpec.addCredentials(creds.get(0), creds.get(1), creds.get(2));
            }
            if (cookie != null) {
                printSpec.addCookie(cookie.get(0), cookie.get(1), cookie.get(2));
            }

            System.out.print("rendering... " + printSpec);
            if (img) {
                writer = new ImageWriter("png");
            } else {
                writer = new PDFWriter();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            writer.write(printSpec, fileOutputStream);

            System.out.println("done " + outputFile);
            if (loop) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(outputFile);
                }
                System.out.println("press enter to run again: 'q' to quit");
                String line = br.readLine();
                if ("q".equals(line)) {
                    break;
                }
                try {
                    ppd = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                }
            } else {
                break;
            }
        }
    }

}
