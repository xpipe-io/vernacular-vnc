package com.shinyhut.vernacular.protocol.messages;

import com.shinyhut.vernacular.client.exceptions.InvalidMessageException;
import com.shinyhut.vernacular.client.exceptions.VncException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;

public class ServerCutText {

    private final String text;

    public ServerCutText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static ServerCutText decode(InputStream in) throws IOException, VncException {
        DataInputStream dataInput = new DataInputStream(in);
        dataInput.readFully(new byte[4]);

        int textLength = dataInput.readInt();

        if (textLength < 0) {
            // The extended format data has to contain at least the flags
            if (textLength == Integer.MIN_VALUE || -textLength < 4) {
                throw new InvalidMessageException("ServerCutText");
            }
            return decodeExtendedMessageFormat(dataInput, -textLength);
        }

        return decodeOriginalFormat(dataInput, textLength);
    }

    private static ServerCutText decodeOriginalFormat(DataInputStream dataInput, Integer textLength) throws IOException {
        byte[] textBytes = new byte[textLength];
        dataInput.readFully(textBytes);
        String text = new String(textBytes, StandardCharsets.ISO_8859_1);
        return new ServerCutText(text);
    }

    private static ServerCutText decodeExtendedMessageFormat(DataInputStream dataInput, Integer textLength) throws IOException, VncException {
        int flags = dataInput.readInt();

        byte[] data = new byte[textLength - 4];
        dataInput.readFully(data);

        if ((flags & MessageHeaderFlags.CAPS.code) != 0) {
            return new ServerCutText("");
        }

        // Text is always the first format in the data if present, so other formats can be ignored
        if ((flags & MessageHeaderFlags.PROVIDE.code) == 0 || (flags & MessageHeaderFlags.TEXT.code) == 0) {
            return new ServerCutText("");
        }

        // Decompress only as much as is needed instead of allocating a fixed size buffer
        try (var inflated = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
            int length = inflated.readInt();
            if (length < 0) {
                throw new InvalidMessageException("ServerCutText");
            }

            // Reads in chunks, so an invalid large length does not allocate a large buffer upfront
            byte[] textBytes = inflated.readNBytes(length);
            if (textBytes.length != length) {
                throw new InvalidMessageException("ServerCutText");
            }

            // The text is null terminated
            int textEnd = length > 0 && textBytes[length - 1] == 0 ? length - 1 : length;
            return new ServerCutText(new String(textBytes, 0, textEnd, StandardCharsets.UTF_8));
        }
    }

}
