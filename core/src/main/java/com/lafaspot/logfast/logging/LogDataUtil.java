package com.lafaspot.logfast.logging;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Utility class used to facilitate logging.
 * 
 * @author lafa
 *
 */
@NotThreadSafe
public class LogDataUtil {

  private static final Object SEPARATOR = " ";
  private Class<?> clazz;
  private Object[] arguments;

  public LogDataUtil set(final Class<?> clazz, final Object... arguments) {
    this.clazz = clazz;
    this.arguments = arguments;
    return this;
  }

  @Override
  public String toString() {
    final StringBuffer str = new StringBuffer();
    str.append(clazz.toString());
    for (final Object obj : arguments) {
      str.append(SEPARATOR);
      if (obj != null) {
        str.append(obj.toString());
      } else {
        str.append("null");
      }
    }
    return str.toString();
  }

}
