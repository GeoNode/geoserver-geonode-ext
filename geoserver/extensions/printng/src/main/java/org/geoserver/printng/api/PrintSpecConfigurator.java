package org.geoserver.printng.api;

import org.geoserver.printng.MessageCollector;
import org.geoserver.printng.spi.PrintSpecException;
import org.geotools.util.logging.Logging;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public abstract class PrintSpecConfigurator<T> {
    
    protected final T source;
    protected final MessageCollector messages;
    
    protected PrintSpecConfigurator(T source) {
        this.source = source;
        messages = new MessageCollector(Logging.getLogger(getClass()));
    }
    
    private void checkValid() throws PrintSpecException {
        String msg = messages.getCombinedErrorMessage();
        if (msg != null) {
            throw new PrintSpecException(msg);
        }
    }
    
    public final PrintSpec configure(PrintSpec spec) throws PrintSpecException {
        configureSpec(spec);
        checkValid();
        return spec;
    }

    protected abstract void configureSpec(PrintSpec spec);
    
}
