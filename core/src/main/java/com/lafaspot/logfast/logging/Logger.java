package com.lafaspot.logfast.logging;

import javax.annotation.concurrent.NotThreadSafe;

import com.lafaspot.logfast.logging.internal.LogPage;
import com.lafaspot.logfast.logging.internal.LogPageRef;

/**
 * Logger is a implementation to be used in multi-threaded application. The main goal of this Logger is to reduce log contention between threads and
 * be fast. This class is not thread safe by design.
 *
 * @author lafa
 */
/**
 * @author lafa
 *
 */
@NotThreadSafe
public class Logger {
    // int values for faster speed
    static final int FATALINT = 1;
    static final int ERRORINT = 2;
    static final int WARNINT = 3;
    static final int INFOINT = 4;
    static final int DEBUGINT = 5;
    static final int TRACEINT = 6;

    /**
     * The class defines the various log levels at which the data needs to be logged.
     *
     * @author lafa
     *
     */
    public enum Level {

        /**
         * fatal.
         */
        FATAL(FATALINT),

        /**
         * error.
         */
        ERROR(ERRORINT),

        /**
         * warn.
         */
        WARN(WARNINT),

        /**
         * info.
         */
        INFO(INFOINT),

        /**
         * debug.
         */
        DEBUG(DEBUGINT),

        /**
         * trace.
         */
        TRACE(TRACEINT);

        Level(final int numeric) {
            this.numeric = numeric;
        }

        /**
         * @return the Level numeric value
         */
        public int getNumeric() {
            return numeric;
        }

        /**
         * Return the log level corresponding to the integer value passed in as {@code numeric}.
         *
         * @param numeric
         *            the numeric value for which the log level needs to be returned.
         * @return the log level corresponding to the numeric value passed in. Returns the default level if the numeric value does not correspond to a
         *         log level.
         */
        public static Level fromNumeric(final int numeric) {
            switch (numeric) {
            case FATALINT:
                return FATAL;

            case ERRORINT:
                return ERROR;

            case WARNINT:
                return WARN;

            case INFOINT:
                return INFO;

            case DEBUGINT:
                return DEBUG;

            case TRACEINT:
                return TRACE;
            default:
                return INFO;
            }
        }

        private int numeric;
    }

    private final org.slf4j.Logger logger;
    private final LogManager manager;
    private LogPageRef currentPageRef;
    private final LogContext context;
    private final boolean legacy;
    private volatile int curLevel;
    private final boolean isDumpStackOn;

    /**
     * @return log level
     */
    public int getLevel() {
        return curLevel;
    }

    /**
     * @param level
     *            log level
     */
    public void setLevel(final Level level) {
        curLevel = level.getNumeric();
    }

    /**
     * Don't make this method public. - lafa
     *
     * @param context
     *            the LogContext for the logger instance
     * @param level
     *            default Level for this logger instance
     * @param manager
     *            the manager instance that owns this logger instance
     */
    protected Logger(final LogContext context, final Level level, final LogManager manager) {
        logger = org.slf4j.LoggerFactory.getLogger(context.getName());
        this.manager = manager;
        this.context = context;
        legacy = manager.isLegacy();
        curLevel = level.getNumeric();
        isDumpStackOn = manager.isDumpStackOn();
        currentPageRef = LogPageRef.NULL;
    }

    private void rotate() {
        LogPage page = currentPageRef.get();
        if (page == null) {
            currentPageRef = manager.allocPage(this, context);
            page = currentPageRef.get();
        }

        if (page != null && page.isFull()) {
            manager.returnPage(this, context, currentPageRef);
            currentPageRef = manager.allocPage(this, context);
        }
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void error(final Object data, final Throwable e) {
        log(Logger.ERRORINT, data, e);
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void warn(final Object data, final Throwable e) {
        log(Logger.WARNINT, data, e);
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void info(final Object data, final Throwable e) {
        log(Logger.INFOINT, data, e);
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void debug(final Object data, final Throwable e) {
        log(Logger.DEBUGINT, data, e);
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void fatal(final Object data, final Throwable e) {
        log(Logger.FATALINT, data, e);
    }

    /**
     * @param data
     *            log data object
     * @param e
     *            exception
     */
    public void trace(final Object data, final Throwable e) {
        log(Logger.TRACEINT, data, e);
    }

    private void log(final int level, final Object data, final Throwable e) {
        if (level <= curLevel && context != null && data != null) {
            rotate();
            // No LogPage no logs. Be fast in case LogPages are not available.
            LogPage page = currentPageRef.get();
            if (page != null) {
                if (legacy) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(context.toString() + " " + data.toString(), e);
                    }
                }
                page.log(context, level, data, e, isDumpStackOn);
            }
        }
    }

    /**
     * @return true if level is on
     */
    public boolean isFatal() {
        return isLevel(Logger.FATALINT);
    }

    /**
     * @return true if level is on
     */
    public boolean isError() {
        return isLevel(Logger.ERRORINT);
    }

    /**
     * @return true if level is on
     */
    public boolean isWarn() {
        return isLevel(Logger.WARNINT);
    }

    /**
     * @return true if level is on
     */
    public boolean isInfo() {
        return isLevel(Logger.INFOINT);
    }

    /**
     * @return true if level is on
     */
    public boolean isDebug() {
        return isLevel(Logger.DEBUGINT);
    }

    /**
     * @return true if level is on
     */
    public boolean isTrace() {
        return isLevel(Logger.TRACEINT);
    }

    private boolean isLevel(final int level) {
        return level <= curLevel;
    }

    /**
     * Call this in case the logger is not going to be used any more or for a long time.
     */
    public void flush() {
        manager.returnPage(this, context, currentPageRef);
        currentPageRef = LogPageRef.NULL;
    }

    protected LogPageRef getCurrentPage() {
        return currentPageRef;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        flush();
    }

}
