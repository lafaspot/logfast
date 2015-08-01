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

  public LogManager() {
    this(Level.INFO, LogPage.DEFAULT_SIZE);
  }

  public LogManager(final Level level, final int size) {
    this.level = level;
    isLegacy = false;
    pages = new ArrayList<LogPage>(size);
  }

  public void setLevel(final Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }

  public Logger getLogger(final LogContext context) {
    return new Logger(context, level, this);
  }

  // 1 Megabyte
  private static final int SIZE = 1 * 1024 * 1024;

  public LogPage allocPage(final Logger logger, final LogContext context) {
    final LogPage page = new LogPage(SIZE);
    pages.add(page);
    return page;
  }

  public void returnPage(final Logger logger, final LogContext context, final LogPage logPage) {
    // TODO Auto-generated method stub
  }

  public boolean isDumpStackOn() {
    return true;
  }

  public byte[] getBytes() {
    return pages.get(0).getBytes();
  }

  public boolean isLegacy() {
    return isLegacy;
  }

  public void setLegacy(final boolean isLegacy) {
    this.isLegacy = isLegacy;
  }

}
