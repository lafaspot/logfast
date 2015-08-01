package com.lafaspot.logfast.logging;

import javax.annotation.concurrent.NotThreadSafe;

import com.lafaspot.logfast.logging.internal.LogPage;

/**
 * Logger is a implementation to be used in multi-threaded application. The main goal of this Logger is to reduce log contention between threads and
 * be fast. This class is not thread safe by design.
 *
 * @author lafa
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
    public static enum Level {
        FATAL(FATALINT), ERROR(ERRORINT), WARN(WARNINT), INFO(INFOINT), DEBUG(DEBUGINT), TRACE(TRACEINT);

        private Level(final int numeric) {
            this.numeric = numeric;
        }

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
            }

            return INFO;
        }

        private int numeric;
    }

    private static final int PAGE_SIZE = 3;
    private final org.slf4j.Logger logger;
    private final LogManager manager;
    private final LogPage[] pages = new LogPage[PAGE_SIZE];
    private int active;
    private final LogContext context;
    private final boolean legacy;
    private volatile int curLevel;
    private final boolean isDumpStackOn;

    public int getLevel() {
        return curLevel;
    }

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
        active = -1;
        curLevel = level.getNumeric();
        isDumpStackOn = manager.isDumpStackOn();
    }

    private void rotate() {
        if (active == -1) {
            active = 0;
            for (int i = 0; i < pages.length; i++) {
                pages[i] = manager.allocPage(this, context);
            }
            return;
        }
        if (pages[active].isFull()) {
            manager.returnPage(this, context, pages[active]);
            pages[active] = null;
            active++;
            // Get more pages to work on
            if (active > PAGE_SIZE) {
                for (int i = 0; i < pages.length; i++) {
                    pages[i] = manager.allocPage(this, context);
                }
                active = 0;
            }
        }
    }

    public void error(final Object data, final Exception e) {
        log(Logger.ERRORINT, data, e);
    }

    public void warn(final Object data, final Exception e) {
        log(Logger.WARNINT, data, e);
    }

    public void info(final Object data, final Exception e) {
        log(Logger.INFOINT, data, e);
    }

    public void debug(final Object data, final Exception e) {
        log(Logger.DEBUGINT, data, e);
    }

    public void fatal(final Object data, final Exception e) {
        log(Logger.FATALINT, data, e);
    }

    public void trace(final Object data, final Exception e) {
        log(Logger.TRACEINT, data, e);
    }

    private void log(final int level, final Object data, final Exception e) {
        if (level <= curLevel && context != null && data != null) {
            rotate();
            if (legacy) {
                logger.debug(context.toString() + " " + data.toString(), e);
            }
            pages[active].log(context, level, data, e, isDumpStackOn);
        }
    }

    public boolean isFatal() {
        return isLevel(Logger.FATALINT);
    }

    public boolean isError() {
        return isLevel(Logger.ERRORINT);
    }

    public boolean isWarn() {
        return isLevel(Logger.WARNINT);
    }

    public boolean isInfo() {
        return isLevel(Logger.INFOINT);
    }

    public boolean isDebug() {
        return isLevel(Logger.DEBUGINT);
    }

    public boolean isTrace() {
        return isLevel(Logger.TRACEINT);
    }

    private boolean isLevel(final int level) {
        return level <= curLevel;
    }

    /**
     * Call this in case the logger is not going to be used any more or for a long time
     */
    public void flush() {
        for (int i = 0; i < pages.length; i++) {
            manager.returnPage(this, context, pages[i]);
            pages[i] = null;
            active = -1;
        }
    }

}
