package com.lafaspot.logfast.logging.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * A Mix between a Strong reference for some time and soft reference after some time.
 *
 * @author lafa
 *
 */
public class LogPageRef extends SoftReference<LogPage>implements Comparable<LogPageRef> {

    /**
     * A NULL softReference that can be used multiple times to save memory allocations.
     */
    public static final LogPageRef NULL = new LogPageRef(null);
    @SuppressWarnings("unused")
    private LogPage page;
    @SuppressWarnings("unused")
    private final long timestamp;
    private final Long pageIdentifier;

    /**
     * Creates a new soft reference that refers to the given object and is registered with the given queue.
     *
     * @param referent
     *            object the new soft reference will refer to
     * @param q
     *            the queue with which the reference is to be registered, or <span>null</span> if registration is not required
     *
     */
    public LogPageRef(final LogPage referent, final ReferenceQueue<? super LogPage> q) {
        super(referent, q);
        this.timestamp = System.currentTimeMillis();
        this.page = referent;
        if (referent == null) {
            this.pageIdentifier = 0L;
        } else {
            this.pageIdentifier = referent.getIdentifier();
        }
    }

    /**
     * Creates a new soft reference that refers to the given object. The new reference is not registered with any queue.
     *
     * @param referent
     *            object the new soft reference will refer to
     */
    public LogPageRef(final LogPage referent) {
        super(referent);
        this.timestamp = System.currentTimeMillis();
        this.page = referent;
        if (referent == null) {
            this.pageIdentifier = 0L;
        } else {
            this.pageIdentifier = referent.getIdentifier();
        }
    }

    @Override
    public void clear() {
        page = null;
        super.clear();
    }

    @Override
    public int compareTo(final LogPageRef other) {
        return Long.compare(this.pageIdentifier, other.pageIdentifier);
    }
}
