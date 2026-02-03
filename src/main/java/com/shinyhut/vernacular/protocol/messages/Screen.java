package com.shinyhut.vernacular.protocol.messages;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Screen implements Encodable {

    private final int id;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Screen(int id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void encode(OutputStream out) throws IOException {
        DataOutput dataOutput = new DataOutputStream(out);
        dataOutput.writeInt(id);
        dataOutput.writeShort(x);
        dataOutput.writeShort(y);
        dataOutput.writeShort(width);
        dataOutput.writeShort(height);
        dataOutput.writeInt(0);
    }
}
