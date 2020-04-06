package org.geoserver.printng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Provide support for geoserver but allow tests to run without it.
 *
 * Note there is a hard-coded coupling of the output url mapping and the
 * physical storage location - don't change one without the other.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class GeoserverSupport {

    static {
        startCleaner();
    }

    /**
     * Compute the relative URI for the provided absolute file path. This does
     * not include any context information (i.e. /geoserver/)
     *
     * @param path Absolute path to output file
     * @return relative URI string
     * @throws IOException
     */
    public static String getOutputFileURI(String path) throws IOException {
        String uri = path.replace(getDirectory("").getParentFile().getAbsolutePath(), "");
        return uri.replace('\\', '/');
    }

    /**
     * Get a random output file with the provided extension.
     *
     * @param ext
     * @return non-null File with provided extension as suffix
     * @throws IOException
     */
    public static File getOutputFile(String ext) throws IOException {
        return new File(getOutputDirectory(),
                UUID.randomUUID().toString() + "." + ext);
    }

    /**
     * Get the 'output' directory - this is for temporary output to support the
     * JSON response type.
     *
     * @return non-null File
     * @throws IOException
     */
    private static File getOutputDirectory() throws IOException {
        return getDirectory("output");
    }

    /**
     * Get the template storage directory
     *
     * @return non-null File
     * @throws IOException
     */
    public static File getPrintngTemplateDirectory() throws IOException {
        return getDirectory("templates");
    }

    /**
     * Get a printng directory - this will be under $datadir/printng or in the
     * system tempdir if geoserver is not running (unit test mode).
     *
     * @param path
     * @return non-null File
     * @throws IOException
     */
    private static File getDirectory(String path) throws IOException {
        GeoServerDataDirectory dataDir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        File dir;
        if (dataDir == null) {
            dir = new File(System.getProperty("java.io.tmpdir"), "printng/" + path);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Error creating template dir: " + dir.getPath());
            }
        } else {
            dir = dataDir.findOrCreateDir("printng", path);
        }
        return dir;
    }

    public static Writer newTemplateWriter(String templateName) throws IOException {
        File directory = GeoserverSupport.getPrintngTemplateDirectory();
        File template = new File(directory, templateName);
        return new FileWriter(template);
    }

    /**
     * Clean any files in the output directory older than maxAge
     *
     * @param maxAge number of millis that determines whether file is 'old'
     */
    public static void cleanOutput(long maxAge) {
        try {
            cleanDirectory(getOutputDirectory(), maxAge);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Clean any files in the specified directory older than maxAge
     *
     * @param maxAge number of millis that determines whether file is 'old'
     */
    public static void cleanDirectory(File directory, long maxAge) {
        File[] children = directory.listFiles();
        long now = System.currentTimeMillis();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                long then = children[i].lastModified();
                if (now - then > maxAge) {
                    children[i].delete();
                }
            }
        }
    }

    /**
     * @todo - if/when ever this type of activity has support from geoserver at
     * a more formalized level (a common task pool anyone?) either remove or fix
     * to use that.
     */
    private static void startCleaner() {
        final long period = 30 * 60 * 1000;
        final File outputDir;
        try {
            outputDir = getOutputDirectory();
        } catch (IOException ex) {
            // this should't happen, if it does, there are probably no files there
            System.err.println("Could not find output directory");
            ex.printStackTrace();
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleanDirectory(outputDir, period);
                    } catch (RuntimeException re) {
                        re.printStackTrace(System.err);
                    }
                    try {
                        cleanDirectory(PrintSupport.getGlobalCacheDir(), period);
                    } catch (RuntimeException re) {
                        re.printStackTrace(System.err);
                    }
                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException ie) {
                        System.err.println("interrupt");
                    }
                }
            }
        });
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
