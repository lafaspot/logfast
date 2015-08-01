package com.lafaspot.logfast.logging;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * LogContext is a object that describes the static data for log calls across multiple logs calss, this is used to reduce memory foot print and allow
 * for in memory lookups. User of this class should implemented it to be immutable.
 *
 * @author lafa
 */
@ThreadSafe
@Immutable
public abstract class LogContext {
    private final String serial;
    private final String name;

    public LogContext(final String name) {
        this.name = name;
        serial = getSerial();
    }

    /**
     * Name for this context. The name is used for look ups and find log pages that are related to a context with the same name.
     *
     * @return the name of this LogContext
     */
    public final String getName() {
        return name;
    }

    /**
     * Return the context serialize to a String object. Sub classes should overwrite this method.
     *
     * @return a string with a serialized version that is going to be written to a log
     */
    public String getSerial() {
        return "{" + getName() + "}";
    }

    /*
     * Returns the cached version of the serialization of the context performed by the getSerial method.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return serial;
    }

}
