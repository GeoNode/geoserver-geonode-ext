package org.geoserver.printng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.geoserver.printng.api.PrintSpec;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.NaiveUserAgent;

/**
 * Extend the NaiveUserAgent with multi-threaded image resolution and optional caching.
 * @todo interaction with the resolveAndOpenStream needs to be investigated
 *       as it's possible that after running preload(), the httpClient connection
 *       pool will be closed.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintUserAgentCallback extends NaiveUserAgent {
    private final Map<String, ImageResource> cache = new HashMap<String, ImageResource>();
    private final UserAgentCallback callback;
    private final PrintSpec spec;
    private final File cacheDir;

    private final static Logger logger = Logging.getLogger(PrintUserAgentCallback.class);
    private static final int MONITOR_JOIN_TIMEOUT_MILLISECONDS = 1000;
    private static final int MONITOR_IDLE_TIMEOUT_SECONDS = 30;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 40;
    private static final int MAX_CONNECTIONS_TOTAL = 500;
    
    /**
     * Monitor to manage old connections in the shared, static Http Connection Pool.
     * Implementation inspired by the blog post at http://www.baeldung.com/httpclient-connection-management
     */
    private static class IdleConnectionMonitorThread extends Thread {
        private final PoolingHttpClientConnectionManager connMgr;
        private volatile boolean shutdown = false;
        
        public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }
        @Override
        public void run() {
            try {
                while (!this.shutdown){
                    synchronized (this) {
                        wait(MONITOR_IDLE_TIMEOUT_SECONDS * 2 * 1000);
                        this.connMgr.closeExpiredConnections();
                        this.connMgr.closeIdleConnections(MONITOR_IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        logger.fine(this.connMgr.getTotalStats().toString());
                    }                   
                }
            } catch(InterruptedException ex) {
                shutdown();
            }
        }
        public void shutdown() {
            this.shutdown = true;
            synchronized(this) {
                notifyAll();
                logger.info("Shutting down the " + PrintUserAgentCallback.class + " Http Connection Pool");
                this.connMgr.shutdown();
                this.connMgr.close();
            }
        }
    }
    
    
    private final static HttpClient httpClient;

    static {
        final PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
        connMgr.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        connMgr.setMaxTotal(MAX_CONNECTIONS_TOTAL);

        httpClient = HttpClientBuilder.create().setConnectionManager(connMgr).useSystemProperties().build();
        
        IdleConnectionMonitorThread staleMonitor = new IdleConnectionMonitorThread(connMgr);
        staleMonitor.start();
        try {
            staleMonitor.join(MONITOR_JOIN_TIMEOUT_MILLISECONDS);
        }
        catch(InterruptedException ex) {
            logger.log(Level.WARNING, "Unexpected interruption during connection pool monitor setup" , ex);
        }
    }

    public PrintUserAgentCallback(PrintSpec spec, UserAgentCallback callback) {
        this.spec = spec;
        this.callback = callback;
        this.cacheDir = spec.getCacheDir();
        assert this.cacheDir != null;
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new RuntimeException("Error creating cache dirs: " + cacheDir.getPath());
        }
        setBaseURL(spec.getBaseURL());
    }
    
    /**
     * Create a ImageResourceLoader that resolves images using the internal
     * cache - this is a workaround for image output rendering only.
     * @return ImageResourceLoader 
     */
    public ImageResourceLoader createImageResourceLoader() {
        return new ImageResourceLoader() {

            @Override
            public synchronized ImageResource get(String uri, int width, int height) {
                ImageResource resource = PrintUserAgentCallback.this.getImageResource(uri);
                if (resource != null) {
                    // tell the loader this image has been loaded
                    loaded(resource, -1, -1);
                    // this will ensure that the loaded image gets scaled using
                    // the internal algorithm - no sense rewriting that
                    resource = super.get(uri, width, height);
                }
                return resource;
            }
            
        };
    }

    private List<Element> getImages() {
        // special hack to optimize open layers map processing
        // not needed anymore since client is stripping hidden elements
        // but possibly still useful
        List<Element> images = new ArrayList<Element>();
        NodeList imgs = spec.getDocument().getElementsByTagName("img");
        nextimg:
        for (int i = 0; i < imgs.getLength(); i++) {
            Element el = (Element) imgs.item(i);
            String style = ((Element) el.getParentNode()).getAttribute("style");
            if (style != null) {
                String[] parts = style.split(";");
                for (int j = 0; j < parts.length; j++) {
                    String[] chunks = parts[j].split(":");
                    if (chunks[0].trim().equals("display")) {
                        if (chunks[1].trim().equals("none")) {
                            break nextimg;
                        }
                    }
                }
            }
            images.add(el);
        }
        return images;
    }

    public void preload() throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("inconceivable", ex);
        }
        List<String> imagesToResolve = new ArrayList<String>();
        List<File> cacheDestination = new ArrayList<File>();
        List<Element> elements = getImages();
        final boolean debug = logger.isLoggable(Level.FINE);
        for (int i = 0; i < elements.size(); i++) {
            String href = elements.get(i).getAttribute("src");
            File cacheFile;
            try {
                String b64 = new BigInteger(digest.digest(href.getBytes())).toString(16);
                cacheFile = new File(cacheDir, b64);
            } catch (Exception ex) {
                throw new RuntimeException("inconceivable", ex);
            }
            if (!cacheFile.exists()) {
                imagesToResolve.add(href);
                cacheDestination.add(cacheFile);
            } else {
                if (debug) {
                    logger.fine("using cache for " + href);
                }
                try {
                    cache(href, callback.getImageResource(cacheFile.toURI().toString()));
                } catch (Exception ex) {
                    throw new RuntimeException("inconceivable", ex);
                }
            }
        }
        if (!imagesToResolve.isEmpty()) {
            ExecutorService threadPool = Executors.newFixedThreadPool(2);
            try {
                ExecutorCompletionService<File> executor = new ExecutorCompletionService<File>(threadPool);
                List<Future<File>> futures = new ArrayList<Future<File>>(imagesToResolve.size());
                for (int i = 0; i < imagesToResolve.size(); i++) {
                    final String href = imagesToResolve.get(i);
                    final File dest = cacheDestination.get(i);
                    futures.add(executor.submit(new Callable<File>() {
                        public File call() throws Exception {
                            return resolve(href, dest);
                        }
                    }));
                }
                for (int i = 0; i < futures.size(); i++) {
                    String resource = imagesToResolve.get(i);
                    File result = null;
                    try {
                        result = futures.get(i).get();
                    } catch (InterruptedException ex) {
                        // this shouldn't happen, but could
                        break;
                    } catch (ExecutionException ex) {
                        // the execution exception just wraps the original
                        throw new RuntimeException("Error resolving image resource " + resource, ex.getCause());
                    }
                    if (result != null) {
                        try {
                            cache(resource, callback.getImageResource(result.toURI().toString()));
                        } catch (Exception ex) {
                            throw new RuntimeException("Error reading resource " + resource, ex);
                        }
                    }
                }
            }
            finally {
                threadPool.shutdown();
            }
        }
    }
    
    @Override
    protected InputStream resolveAndOpenStream(String uriSpec) {
        InputStream is = null;
        try {        
            URI resolved = new URI(resolveURI(uriSpec));
            if (resolved.getScheme().equals("file")) {
                is = resolved.toURL().openStream();
            } else {
                is = resolveAndOpenRemoteStream(resolved.toString());
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error resolving : " + uriSpec, ex);
        }
        return is;
    }
    
    private InputStream resolveAndOpenRemoteStream(String uri) throws Exception {
        HttpGet get = new HttpGet(uri);
        InputStream is = null;
        String host = get.getURI().getHost();
        Cookie cookie = spec.getCookie(host);
        PasswordAuthentication creds = spec.getCredentials(host);
        final boolean debug = logger.isLoggable(Level.FINE);
        final HttpClientContext context = HttpClientContext.create();
        if (creds != null) {
            if (debug) {
                logger.fine("setting credentials for " + host);
            }
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(creds.getUserName(),
                    new String(creds.getPassword()));
            credsProvider.setCredentials(AuthScope.ANY, credentials);

            AuthCache authCache = new BasicAuthCache();
            HttpHost targetHost = new HttpHost(host);
            authCache.put(targetHost, new BasicScheme());

            // Add AuthCache to the execution context
            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);

            context.setCookieStore(new BasicCookieStore());
        }

        // even if using basic auth, disable cookies
        context.setCookieStore(new BasicCookieStore());
        if (cookie != null) {
            if (debug) {
                logger.fine("setting cookie for " + host + " to " + cookie);
            }
            // this made things work - not sure what I was doing wrong with
            // other cookie API
            context.getCookieStore().addCookie(cookie);
            // get.setRequestHeader("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        if (debug) {
            logger.fine("fetching " + uri);
        }

        HttpResponse response = httpClient.execute(get, context);
        if (response.getStatusLine().getStatusCode() == 200) {
            is = response.getEntity().getContent();
        } else {
            logger.warning("Error fetching : " + uri + ", status is : " + response.getStatusLine().getStatusCode());
            if (response.getEntity() != null) {
                logger.log(Level.FINE, "Response : {0}", EntityUtils.toString(response.getEntity()));
            }
        }
        return is;
    }

    private File resolve(String href, File dest) throws Exception {
        href = href.trim();
        if (href.length() == 0) {
            return null;
        }
        InputStream in = resolveAndOpenStream(href);
        File retval = null;
        if (in != null) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(dest);
                IOUtils.copy(in, fout);
            } finally {
                if (fout != null) {
                    fout.close();
                }
                in.close();
            }
            retval = dest;
        }
        return retval;
    }

    private void warn(String uri) {
        logger.warning("could not resolve " + uri);
    }

    @Override
    public byte[] getBinaryResource(String uri) {
        byte[] resource = super.getBinaryResource(uri);
        if (resource == null) {
            warn(uri);
        }
        return resource;
    }

    @Override
    public ImageResource getImageResource(String uri) {
        ImageResource r = cache.get(uri);
        if (r == null || r.getImage() == null) {
            r = callback.getImageResource(uri);
        }
        if (r == null) {
            warn(uri);
        }
        return r;
    }

    public void cleanup() {
        // @todo when caching is done more intelligently, implement this
    }

    private void cache(String resource, ImageResource imageResource) {
        cache.put(resource, new ImageResource(resource, imageResource.getImage()));
    }
    
}
