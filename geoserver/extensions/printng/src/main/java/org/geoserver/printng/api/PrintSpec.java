package org.geoserver.printng.api;

import java.io.File;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.geoserver.printng.PrintSupport;
import org.geoserver.printng.spi.ParsedDocument;
import org.w3c.dom.Document;

public class PrintSpec {
    
    private int width;
    private int height;
    private int outputWidth;
    private int outputHeight;
    private int dpp;
    private File cacheDir;
    private Map<String, PasswordAuthentication> credentials;
    private Map<String, Cookie> cookies;
    private final ParsedDocument parser;
    private String cssOverride = null;

    public PrintSpec(ParsedDocument parser) {
        dpp = 20;
        width = -1;
        height = -1;
        outputWidth = -1;
        outputHeight = -1;
        credentials = new HashMap<String, PasswordAuthentication>();
        cookies = new HashMap<String, Cookie>();
        this.parser = parser;
    }

    public String getCssOverride() {
        return cssOverride;
    }

    public void setCssOverride(String cssOverride) {
        this.cssOverride = cssOverride;
    }

    /**
     * Get the base URL for resolving resources for the document.
     * @return null if none specified
     */
    public String getBaseURL() {
        return parser.getBaseURL();
    }
    
    public Document getDocument() {
        if (cssOverride != null) {
            parser.addCssOverride(cssOverride);
        }
        return parser.getDocument();
    }
    
    public Cookie getCookie(String host) {
        return cookies.get(host);
    }
    
    public PasswordAuthentication getCredentials(String host) {
        return credentials.get(host);
    }
    
    public void addCredentials(String host, String user, String pass) {
        PasswordAuthentication existing = 
                credentials.put(host, new PasswordAuthentication(user, pass.toCharArray()));
        if (existing != null) {
            throw new IllegalArgumentException("host credentials already exist for " + host);
        }
    }
    
    public void addCookie(String host, String key, String value) {
        Cookie existing = cookies.put(host, new BasicClientCookie(key, value));
        if (existing != null) {
            throw new IllegalArgumentException("host cookie already exists for " + host);
        }
    }

    /**
     * For image rendering, this is the intended native width.
     * @return -1 if not known or set
     */
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * For image rendering, this is the intended native height.
     * @return -1 if not known or set
     */
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    
    public boolean isOutputDimensionSet() {
        return outputWidth != -1 && outputHeight != -1;
    }
    
    public boolean isRenderDimensionSet() {
        return width != -1 && height != -1;
    }
    
    /**
     * For image rendering, this is the intended final width.
     * @return -1 if not known or set
     */
    public int getOutputWidth() {
        return outputWidth;
    }

    public void setOutputWidth(int outputWidth) {
        this.outputWidth = outputWidth;
    }
    
    /**
     * For image rendering, this is the intended final height.
     * @return -1 if not known or set
     */
    public int getOutputHeight() {
        return outputHeight;
    }

    public void setOutputHeight(int outputHeight) {
        this.outputHeight = outputHeight;
    }
    
    public int getDotsPerPixel() {
        return dpp;
    }

    public void setDotsPerPixel(int dpp) {
        this.dpp = dpp;
    }

    public boolean isCacheDirSet() {
        return this.cacheDir != null;
    }

    public File getCacheDir() {
        // ideally, caching would be configurable per source
        // to support caching common base layers, etc.
        // but for now, it is only to avoid memory issues...
        File cache = cacheDir;
        if (cache == null) {
            cache = PrintSupport.getGlobalCacheDir();
        }
        return cache;
    }

    public void setCacheDirRoot(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * For image rendering, if no intention is specified, use the defaults.
     */
    public PrintSpec useDefaultRenderDimension() {
        if (isOutputDimensionSet()) {
            width = outputWidth;
            height = outputHeight;
        } else if (! isRenderDimensionSet()) {
            outputWidth = width = 512;
            outputHeight = height = 256;
        }
        return this;
    }

    @Override
    public String toString() {
        return "PrintSpec{" + "width=" + width + ", height=" + height + ", outputWidth=" + outputWidth + ", outputHeight=" + outputHeight + ", dpp=" + dpp + ", credentials=" + credentials + ", cookies=" + cookies + '}';
    }
    
}
