package com.shinyhut.vernacular.protocol.messages;

import com.shinyhut.vernacular.utils.AesEaxInputStream;

import java.io.*;

public abstract class Decoder {

    public static Decoder raw(InputStream in) {
        return new Decoder() {
            @Override
            public InputStream getInputStream() throws IOException {
                return in;
            }
        };
    }

    public static Decoder ra2(AesEaxInputStream aes) {
        var in = new InputStream() {

            private byte[] buf;
            private int pos;

            @Override
            public int read() throws IOException {
                if (buf == null || pos == buf.length) {
                    buf = aes.read();
                    pos = 0;
                }

                if (buf.length == 0) {
                    return -1;
                }

                return buf[pos++] & 0xFF;
            }
        };
        return new Decoder() {
            @Override
            public InputStream getInputStream() throws IOException {
                return in;
            }
        };
    }

    public abstract InputStream getInputStream() throws IOException;
}
