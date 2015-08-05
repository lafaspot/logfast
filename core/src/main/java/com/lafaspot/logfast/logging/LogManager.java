package com.lafaspot.logfast.logging;

import java.util.ArrayList;

import javax.annotation.concurrent.ThreadSafe;

import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.logfast.logging.internal.LogPage;

/**
 * Log Manager.
 *
 * @author lafa
 *
 */
@ThreadSafe
public class LogManager {
    private Level level;

    private final ArrayList<LogPage> pages;

    private boolean isLegacy;

    /**
     * Create a log manager instance with level set to Level.INFO.
     */
    public LogManager() {
        this(Level.INFO, LogPage.DEFAULT_SIZE);
    }

    /**
     * Create a log manager instance with level and page size.
     *
     * @param level
     *            log level
     * @param size
     *            page size
     */
    public LogManager(final Level level, final int size) {
        this.level = level;
        isLegacy = false;
        pages = new ArrayList<LogPage>(size);
    }

    /**
     * @param level
     *            log level
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * @return log level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Factory that create logger instances.
     *
     * @param context
     *            the context for this logger instance
     * @return the logger instance
     */
    public Logger getLogger(final LogContext context) {
        return new Logger(context, level, this);
    }

    // 1 Megabyte
    private static final int SIZE = 1 * 1024 * 1024;

    /**
     * Package protected.
     *
     * @param logger
     *            logger
     * @param context
     *            context
     * @return page
     */
    LogPage allocPage(final Logger logger, final LogContext context) {
        final LogPage page = new LogPage(SIZE);
        pages.add(page);
        return page;
    }

    /**
     * Package protected.
     *
     * @param logger
     *            logger
     * @param context
     *            context
     * @param logPage
     *            page
     */
    public void returnPage(final Logger logger, final LogContext context, final LogPage logPage) {
        // TODO Auto-generated method stub
    }

    /**
     * @return if dump full stack is on
     */
    public boolean isDumpStackOn() {
        return true;
    }

    /**
     * @return page bytes in atom
     */
    public byte[] getBytes() {
        return pages.get(0).getBytes();
    }

    /**
     * @return is calling slf4j
     */
    public boolean isLegacy() {
        return isLegacy;
    }

    /**
     * @param isLegacy
     *            call slf4j
     */
    public void setLegacy(final boolean isLegacy) {
        this.isLegacy = isLegacy;
    }

}
