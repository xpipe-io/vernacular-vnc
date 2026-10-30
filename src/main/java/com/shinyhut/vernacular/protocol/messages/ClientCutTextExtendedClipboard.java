package com.shinyhut.vernacular.protocol.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;

import static com.shinyhut.vernacular.protocol.messages.MessageHeaderFlags.PROVIDE;
import static com.shinyhut.vernacular.protocol.messages.MessageHeaderFlags.TEXT;

public class ClientCutTextExtendedClipboard  implements Encodable {

    private final String text;

    public ClientCutTextExtendedClipboard(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public void encode(OutputStream out) throws IOException {
        int flags = PROVIDE.code | TEXT.code;

        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        // Closing the stream also releases the native resources of the deflater
        var compressed = new ByteArrayOutputStream();
        try (var deflated = new DataOutputStream(new DeflaterOutputStream(compressed))) {
            // Null terminated text, the length includes the terminator
            deflated.writeInt(textBytes.length + 1);
            deflated.write(textBytes);
            deflated.writeByte(0);
        }

        DataOutput dataOutput = new DataOutputStream(out);

        dataOutput.writeByte(0x06);
        dataOutput.write(new byte[3]);

        dataOutput.writeInt(-(compressed.size() + 4));
        dataOutput.writeInt(flags);

        dataOutput.write(compressed.toByteArray());
    }
}
