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

    public LogPage(final int pageSize) {
        this.pageSize = pageSize;
        ostream = new ByteArrayOutputStream(pageSize);
        schema = new Schema.Parser().parse(schemaStr);
        writer = new GenericDatumWriter<GenericRecord>(schema);
        encoder = EncoderFactory.get().directBinaryEncoder(ostream, null);
        record = new GenericData.Record(schema);
    }

    public static final String schemaStr = "{ \"type\":\"record\", \"namespace\":\"com.lafaspot.logfast.logging\", \"name\":\"LogRecord\", \"fields\":[ "
            + "{ \"name\":\"name\", \"type\":\"string\", \"default\":\"\" }," + "{ \"name\":\"level\", \"type\":\"int\", \"default\":-1 },"
            + "{ \"name\":\"data\", \"type\":\"string\", \"default\":\"\" },"
            + "{ \"name\":\"eMessages\", \"type\":\"string\", \"default\":\"\" },"
            + "{ \"name\":\"eStackTrace\", \"type\":\"string\", \"default\":\"\"}" + "] }";

    public void log(final LogContext context, final int level, final Object data, final Exception e, final boolean isDumpStackOn) {
        // reset log record
        record.put("name", context.toString());
        record.put("level", level);
        record.put("data", data.toString());
        record.put("eMessages", null);
        record.put("eStackTrace", null);

        // Calculate array of messages and put here

        if (e != null) {
            StringBuffer messages = new StringBuffer();
            Throwable eHelper = e;
            int count = 0;
            // iterate for 10 causes
            while (eHelper != null && count < 10) {
                messages.append("[");
                messages.append(eHelper.getClass().getName());
                messages.append(", ");
                messages.append(eHelper.getMessage());
                messages.append("],");
                eHelper = e.getCause();
                count++;
            }
            record.put("eMessages", messages.toString());

            if (isDumpStackOn) {
                // TODO: Convert stack trace into a String. stack trace can't be null.
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
        } catch (IOException e1) {
            // Ignore exception mark the Page full.
            isFull = true;
        }
    }

    public boolean isFull() {
        return isFull;
    }

    public byte[] getBytes() {
        try {
            encoder.flush();
        } catch (IOException e) {
            // should never happen.
        }
        return ostream.toByteArray();
    }

    public void reset() {
        try {
            encoder.flush();
        } catch (IOException e) {
            // should never happen.
        }
        ostream.reset();
        isFull = false;
    }
}
