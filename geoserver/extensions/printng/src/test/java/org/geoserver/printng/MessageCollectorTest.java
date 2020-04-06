package org.geoserver.printng;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class MessageCollectorTest {
    
    @Test
    public void testCombinedErrorMessage() {
        Logger logger = Logger.getAnonymousLogger();
        MemoryHandler handler = new MemoryHandler();
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
        MessageCollector msgs = new MessageCollector(logger);
        
        assertNull(msgs.getCombinedErrorMessage());
        
        msgs.info("info");
        assertNull(msgs.getCombinedErrorMessage());

        msgs.severe("error");
        msgs.fine("fine");
        msgs.finer("finer");
        
        assertEquals("error", msgs.getCombinedErrorMessage().trim());
        // finer should have been nixed
        assertEquals(3, handler.records.size());
    }
    
    @Test
    public void testTracing() {
        Logger logger = Logger.getAnonymousLogger();
        MemoryHandler handler = new MemoryHandler();
        logger.setLevel(Level.FINEST);
        logger.addHandler(handler);
        MessageCollector msgs = new MessageCollector(logger);
        
        msgs.warning("something that might be useful to debug");
        
        // the trace should be to this method
        StackTraceElement stackTop = handler.records.get(0).getThrown().getStackTrace()[0];
        assertEquals(getClass().getName(), stackTop.getClassName());
        assertEquals("testTracing", stackTop.getMethodName());
    }
    
    static class MemoryHandler extends Handler {
        
        List<LogRecord> records = new ArrayList<LogRecord>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
        
    }
}
