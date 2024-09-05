package com.shinyhut.vernacular.protocol.messages;

import com.shinyhut.vernacular.utils.AesEaxOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Encoder {

    public static Encoder raw(OutputStream out) {
        return new Encoder() {
            @Override
            public void write(byte[] data) throws IOException {
                out.write(data);
            }
        };
    }

    public static Encoder ra2(AesEaxOutputStream aes) {
        return new Encoder() {
            @Override
            public void write(byte[] data) throws IOException {
                aes.write(data);
            }
        };
    }

    public abstract void write(byte[] data) throws IOException;

    public void encode(Encodable encodable) throws IOException {
        var out = new ByteArrayOutputStream();
        encodable.encode(out);
        write(out.toByteArray());
    }
}
