package org.opencb.hpg.bigdata.core.io.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hpccoll1 on 02/04/15.
 */
public class AvroEncoder<T> {

    final DatumWriter<T> datumWriter;
    final Encoder encoder;
    final ByteArrayOutputStream byteArrayOutputStream;
    private int encodeFails = 0;

    public AvroEncoder(Schema schema) {
        this.datumWriter = new GenericDatumWriter<>(schema);
        this.byteArrayOutputStream = new ByteArrayOutputStream(1000000);    //Initialize with 1MB
        this.encoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null);
    }

    public List<ByteBuffer> encode(List<T> batch) throws IOException {
        List<ByteBuffer> encoded = new ArrayList<>(batch.size());
        for (T elem : batch) {
            try {
                datumWriter.write(elem, encoder);
            } catch (Exception e) {
                encodeFails++;
                System.err.println(e.getMessage());
                encoder.flush();
                byteArrayOutputStream.reset();
                continue;
            }
            encoder.flush();
            encoded.add(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()));
            byteArrayOutputStream.reset();
        }
        return encoded;
    }


}
