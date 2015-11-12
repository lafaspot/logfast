package com.lafaspot.logfast.logging.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import com.lafaspot.logfast.logging.LogContext;

/**
 * LogPage.
 *
 * @author lafa
 *
 */
@NotThreadSafe
public class LogPage {
    // Page size in bytes.
    private final int pageSize;
    // Is this page full.
    private boolean isFull = false;
    // ByteArray where the log data is stored.
    private final ByteArrayOutputStream ostream;

    // Avro stuff
    private final Schema schema;
    private final GenericDatumWriter<GenericRecord> writer;
    private final Encoder encoder;
    private final Record record;
    private LogPageRef nextPageRef;
    private boolean isActive;
    private long identifier;

    /**
     * @param pageId
     *            Page Unique Identifier
     * @param pageSize
     *            Max memory size for this page
     * @param nextPageRef
     *            Page reference for the the last full page related to this one
     */
    public LogPage(final long pageId, final int pageSize, final LogPageRef nextPageRef) {
        this.pageSize = pageSize;
        identifier = pageId;
        ostream = new ByteArrayOutputStream(pageSize);
        schema = new Schema.Parser().parse(SCHEMA_STR);
        writer = new GenericDatumWriter<GenericRecord>(schema);
        encoder = EncoderFactory.get().directBinaryEncoder(ostream, null);
        record = new GenericData.Record(schema);
        this.nextPageRef = nextPageRef;
        isActive = true;
    }

    /**
     * avro schema as a String.
     */
    public static final String SCHEMA_STR = "{ \"type\":\"record\", \"namespace\":\"com.lafaspot.logfast.logging\", "
                    + "\"name\":\"LogRecord\", \"fields\":[ " + "{ \"name\":\"name\", \"type\":\"string\", \"default\":\"\" },"
                    + "{ \"name\":\"level\", \"type\":\"int\", \"default\":-1 }," + "{ \"name\":\"data\", \"type\":\"string\", \"default\":\"\" },"
                    + "{ \"name\":\"eMessages\", \"type\":[ \"string\", \"null\"], \"default\":\"\" },"
                    + "{ \"name\":\"eStackTrace\", \"type\":[ \"string\", \"null\"], \"default\":\"\"}" + "] }";

    /**
     * Default page size.
     */
    public static final int DEFAULT_SIZE = 10;
    private static final int EXCEPTION_DEPTH = 10;

    /**
     * @param context
     *            - the LogContext
     * @param level
     *            the default log level for this page
     * @param data
     *            the object obect to be logged
     * @param cause
     *            exception that create the problem
     * @param isDumpStackOn
     *            is true to enable dumping stack in the logs
     */
    public void log(final LogContext context, final int level, final Object data, final Throwable cause, final boolean isDumpStackOn) {
        // reset log record
        record.put("name", context.toString());
        record.put("level", level);
        record.put("data", data.toString());
        record.put("eMessages", null);
        record.put("eStackTrace", null);

        // Calculate array of messages and put here

        if (cause != null) {
            final StringBuffer messages = new StringBuffer();
            Throwable eHelper = cause;
            int count = 0;
            // iterate for 10 causes
            while (eHelper != null && count < EXCEPTION_DEPTH) {
                messages.append("[");
                messages.append(eHelper.getClass().getName());
                messages.append(", ");
                messages.append(eHelper.getMessage());
                messages.append("],");
                eHelper = cause.getCause();
                count++;
            }
            record.put("eMessages", messages.toString());

            if (isDumpStackOn) {
                // TODO: Convert stack trace into a String. stack trace can't be
                // null.
                record.put("eStackTrace", "stack trace here");
            }
        }

        if (isFull) {
            // should never happen.
            throw new RuntimeException("LogPage is Full.");
        }

        try {
            writer.write(record, encoder);
            if (ostream.size() >= pageSize) {
                isFull = true;
            }
        } catch (final IOException e1) {
            // Ignore exception mark the Page full.
            isFull = true;
        }
    }

    /**
     * @return if the page is full
     */
    public boolean isFull() {
        return isFull;
    }

    /**
     * @return the bytes of the log page
     */
    public byte[] getBytes() {
        try {
            encoder.flush();
        } catch (final IOException e) {
            // should never happen.
        }
        return ostream.toByteArray();
    }

    /**
     *
     */
    public void setNotActive() {
        try {
            encoder.flush();
        } catch (final IOException e) {
            // should never happen.
        }
        ostream.reset();
        isFull = false;
        isActive = false;
        nextPageRef = LogPageRef.NULL;
    }

    /**
     * Return the next page or null.
     *
     * @return a LogPageRef to the next page
     */
    public LogPageRef getNextPage() {
        return nextPageRef;
    }

    /**
     * Visit all current pages and clean references below maxPages and reset the LogPages that are unreferenced.
     *
     * @param maxPages
     *            Number of page to keep
     */
    public void removePageRefAboveLimit(final int maxPages) {
        LogPage nPage = nextPageRef.get();
        // Visit all current pages
        if (nPage != null) {
            nPage.removePageRefAboveLimit(maxPages - 1);
        }
        // clean references below maxPages and reset the LogPage
        if (maxPages < 0) {
            nextPageRef.clear();
            nPage.setNotActive();
        }
    }

    /**
     *
     * @param nextPageRef
     *            reconfigure the page to be active again, so it can be reused
     */
    public void setActive(final LogPageRef nextPageRef) {
        this.nextPageRef = nextPageRef;
        isActive = true;
    }

    /**
     * @return If this page is still active returns true otherwise false
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * @return Return the Page Unique Identifier
     */
    public Long getIdentifier() {
        return identifier;
    }
}
