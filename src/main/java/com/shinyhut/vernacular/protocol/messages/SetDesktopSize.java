package com.shinyhut.vernacular.protocol.messages;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class SetDesktopSize implements Encodable {

    private final int width;
    private final int height;
    private final List<Screen> screens;

    public SetDesktopSize(int width, int height, List<Screen> screens) {
        this.width = width;
        this.height = height;
        this.screens = screens;
    }

    @Override
    public void encode(OutputStream out) throws IOException {
        DataOutput dataOutput = new DataOutputStream(out);
        dataOutput.writeByte(251);
        dataOutput.writeByte(0);
        dataOutput.writeShort(width);
        dataOutput.writeShort(height);
        dataOutput.writeByte(screens.size());
        dataOutput.writeByte(0);
        for (Screen screen : screens) {
            screen.encode(out);
        }
    }
}
