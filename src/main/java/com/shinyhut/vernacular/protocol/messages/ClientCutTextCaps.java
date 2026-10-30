package com.shinyhut.vernacular.protocol.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientCutTextCaps  implements Encodable {

    private final Map<MessageHeaderFlags, Integer> sizes;

    public ClientCutTextCaps(Map<MessageHeaderFlags, Integer> sizes) {
        this.sizes = sizes;
    }

    @Override
    public void encode(OutputStream out) throws IOException {
        int flags = MessageHeaderFlags.CAPS.code
                | MessageHeaderFlags.NOTIFY.code | MessageHeaderFlags.PEEK.code
                | MessageHeaderFlags.PROVIDE.code
                | MessageHeaderFlags.REQUEST.code;

        // There is one size entry for each format flag set, ordered by the format bits.
        // Only the lower 16 bits are formats, the others are actions which have no size
        var formatSizes = new ByteArrayOutputStream();
        var formatSizesOutput = new DataOutputStream(formatSizes);
        var formats = sizes.keySet().stream()
                .filter(format -> format.code < (1 << 16))
                .sorted(Comparator.comparingInt(format -> format.code))
                .collect(Collectors.toList());
        for (var format : formats) {
            flags |= format.code;
            formatSizesOutput.writeInt(sizes.get(format));
        }

        DataOutput dataOutput = new DataOutputStream(out);

        dataOutput.writeByte(0x06);
        dataOutput.write(new byte[3]);

        dataOutput.writeInt(formatSizes.size() + 4);
        dataOutput.writeInt(flags);
        dataOutput.write(formatSizes.toByteArray());
    }
}
