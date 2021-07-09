package com.lafaspot.logfast.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import javax.annotation.concurrent.ThreadSafe;

import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.logfast.logging.internal.LogPage;
import com.lafaspot.logfast.logging.internal.LogPageRef;

/**
 * Log Manager.
 *
 * @author lafa
 *
 */
@ThreadSafe
public class LogManager {
    // 1 Megabyte
    private static final int SIZE = 1 * 1024 * 1024;

    private Level level;

    private final Set<LogPageRef> pages;

    private boolean isLegacy;

    private final int maxSize;

    private static final int MAX_LOGGER_PAGES = 3;
    private static final int MIN_PAGE_SIZE = 0;

    private final PagePredicate cleanUnused = new PagePredicate();

    private final AtomicLong createdPages = new AtomicLong(0);

    private final AtomicLong deletedPages = new AtomicLong(0);

    private final AtomicLong lastPageSize = new AtomicLong(0);

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
     *            page size, mininum value is 10 pages
     */
    public LogManager(final Level level, final int size) {
        this.level = level;
        isLegacy = false;
        if (size < MIN_PAGE_SIZE) {
            this.maxSize = MIN_PAGE_SIZE;
        } else {
            this.maxSize = size;
        }
        pages = new ConcurrentSkipListSet<LogPageRef>();
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

    /**
     * Package protected.
     *
     * @param logger
     *            logger
     * @param context
     *            context
     * @return page
     */
    LogPageRef allocPage(final Logger logger, final LogContext context) {
        long lastSize = lastPageSize.get();
        long currentSize = createdPages.get();
        long size = currentSize - lastSize;
        if (size > maxSize && (lastPageSize.compareAndSet(lastSize, currentSize))) {
            cleanPages();
        }
        final LogPage page = new LogPage(createdPages.incrementAndGet(), SIZE, logger.getCurrentPage());
        page.removePageRefAboveLimit(MAX_LOGGER_PAGES);
        LogPageRef pageRef = new LogPageRef(page);
        pages.add(pageRef);
        return pageRef;
    }

    private class PagePredicate implements Predicate<LogPageRef> {
        @Override
        public boolean test(final LogPageRef pageRef) {
            LogPage page = pageRef.get();
            // remove page reference if page is null or the page is full
            if (page != null && !page.isFull()) {
                return false;
            }
            pageRef.clear();
            deletedPages.getAndIncrement();
            return true;
        }
    }

    private class PageRemoveByIdPredicate implements Predicate<LogPageRef> {
        private long minPageId;

        PageRemoveByIdPredicate(final long minPageId) {
            this.minPageId = minPageId;
        }

        @Override
        public boolean test(final LogPageRef pageRef) {

            LogPage page = pageRef.get();
            // remove page reference if page is null or the page is full
            if (page != null && page.getIdentifier() > minPageId) {
                return false;
            }
            pageRef.clear();
            deletedPages.getAndIncrement();
            return true;
        }
    }

    private void cleanPages() {
        pages.removeIf(cleanUnused);
        long size = createdPages.get() - deletedPages.get();
        if (size >= maxSize) {
            // No more memory release 50% of the active pages
            pages.removeIf(new PageRemoveByIdPredicate(createdPages.get() - (maxSize / 2)));
        }
    }

    /**
     * Package protected.
     *
     * @param logger
     *            logger
     * @param context
     *            context
     * @param currentPageRef
     *            page
     */
    public void returnPage(final Logger logger, final LogContext context, final LogPageRef currentPageRef) {
        currentPageRef.clear();
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (LogPageRef pageRef : pages) {
            LogPage page = pageRef.get();
            if (page != null) {
                try {
                    out.write(page.getBytes());
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return out.toByteArray();
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

    /**
     * @author lafa
     *
     */
    public static class Stats {
        private final LogManager manager;

        /**
         * @param manager
         *            the LogManager
         */
        public Stats(final LogManager manager) {
            this.manager = manager;
        }

        /**
         * @return current active pages
         */
        public long activePages() {
            return manager.pages.size();
        }

        /**
         * @return deleted pages
         */
        public long deletedPages() {
            return manager.deletedPages.get();
        }

        /**
         * @return total created pages
         */
        public long createdPages() {
            return manager.createdPages.get();
        }
    }

    /**
     * @return the stats for this manager instance
     */
    public Stats stats() {
        return new Stats(this);
    }

}
