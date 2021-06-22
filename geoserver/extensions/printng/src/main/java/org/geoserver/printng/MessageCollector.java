package org.geoserver.printng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger delegate that collects any severe messages. Any loggable messages are
 * forwarded to the delegate logger. If the delegate logger is set to
 * Level.FINEST, stack traces will be filled in to support debugging.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class MessageCollector extends Logger {
    
    private final Logger delegate;
    private final Level capture;
    private final List<LogRecord> records = new ArrayList<LogRecord>(3);
    
    public MessageCollector(Logger logger, Level capture) {
        super("collector", logger.getResourceBundleName());
        // assume the level of the delegate logger
        setLevel(logger.getLevel());
        this.delegate = logger;
        this.capture = capture;
    }
    
    public MessageCollector(Logger logger) {
        this(logger, Level.SEVERE);
    }
    
    @Override
    public void log(LogRecord record) {
        if (delegate.isLoggable(record.getLevel())) {
            // if the logger has been configured to finest, go ahead and fill in stack traces
            if (record.getThrown() == null && isLoggable(Level.FINEST)) {
                Exception ex = new Exception("Message Collector Trace");
                // trim the top stacks off - they are noise
                StackTraceElement[] stackTrace = ex.getStackTrace();
                String loggerClassName = Logger.class.getName();
                int i = 1;
                for (; i < stackTrace.length; i++) {
                    if (!stackTrace[i].getClassName().equals(loggerClassName)) {
                        break;
                    }
                }
                ex.setStackTrace(Arrays.asList(stackTrace).subList(i, stackTrace.length).toArray(new StackTraceElement[stackTrace.length - i]));
                record.setThrown(ex);
            }
            delegate.log(record);
        }
        records.add(record);
    }
    
    public String getCombinedErrorMessage() {
        List<LogRecord> catpured = getCapturedMessages();
        return catpured.isEmpty() ? null : formatMessages(catpured);
    }
    
    public List<LogRecord> getCapturedMessages() {
        List<LogRecord> errors = new ArrayList<LogRecord>(records.size());
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getLevel().intValue() >= capture.intValue()) {
                errors.add(records.get(i));
            }
        }
        return errors;
    }
    
    private String formatMessages(List<LogRecord> records) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            buf.append(records.get(i).getMessage()).append('\n');
        }
        return buf.toString();
    }
}
