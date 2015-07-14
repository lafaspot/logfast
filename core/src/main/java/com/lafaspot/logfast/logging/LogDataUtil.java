package com.lafaspot.logfast.logging;

import javax.annotation.concurrent.NotThreadSafe;

/**
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
        StringBuffer str = new StringBuffer();
        str.append(clazz.toString());
        try {
            for (Object obj : arguments) {
                str.append(SEPARATOR);
                if (obj != null) {
                    str.append(obj.toString());
                } else {
                    str.append("null");
                }
            }
        } catch (Exception ex) {
            str.append("arguments serialization failed.");
        }
        return str.toString();
    }

}
