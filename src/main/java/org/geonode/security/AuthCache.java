package org.geonode.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
/**
 * Simple cache for caching authorizations for authenticated users in order to not send queries to 
 * GeoNode too frequently.
 * 
 * <p>
 * Setting the timeout for values in cache to a value which is too big can have a nasty side effect has if
 * we change authorizations for a user it will take a long time (in the worst case as long as the cache timeout)
 * to have this reflected on GeoServer due to the delay introduced by the cache itself.
 * 
 * <p>
 * On the other hand a value for the timeout which is too low may result in a performance hit due to the fact that we would be sneding
 * too many requests to GeoNode for loading the authorizations for the current user.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
public class AuthCache {

    private static final Logger LOGGER = Logging.getLogger(AuthCache.class);

    private static final int DEFAULT_TIMEOUT = 300000;

    private ConcurrentHashMap<String, Authentication> cache;

    private final ScheduledExecutorService cacheEvictor;

    private class EvictAuth implements Runnable {

        private final String key;

        public EvictAuth(final String key) {
            this.key = key;
        }

        public void run() {
            Authentication removed = cache.remove(key);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.finer("AuthCache::run: evicted auth '" + key + "': " + removed);
            }
        }

    }

    private long timeout = DEFAULT_TIMEOUT;

    public AuthCache(){
    	this(DEFAULT_TIMEOUT);
    }
    
    public AuthCache(final int timeout) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Creating authentication cache");
        }
        cache = new ConcurrentHashMap<String, Authentication>();

        CustomizableThreadFactory tf = new CustomizableThreadFactory();
        tf.setDaemon(true);
        tf.setThreadNamePrefix("GeoNode Auth Cache Evictor-");
        tf.setThreadPriority(Thread.MIN_PRIORITY + 1);
        cacheEvictor = Executors.newScheduledThreadPool(1, tf);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Authentication get(final String authKey) {
        return cache.get(authKey);
    }

    public void put(final String authKey, final Authentication auth) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthCache::put: Adding authentication to cache: "+auth);
        }    	
        Assert.notNull(auth);
        cache.put(authKey, auth);
        cacheEvictor.schedule(new EvictAuth(authKey), timeout, TimeUnit.MILLISECONDS);
    }

}
