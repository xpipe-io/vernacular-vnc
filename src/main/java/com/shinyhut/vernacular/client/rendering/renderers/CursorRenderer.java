package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ImageBuffer;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.shinyhut.vernacular.utils.ByteUtils.bitAt;

public class CursorRenderer implements Renderer {

    private final RawRenderer rawRenderer;

    public CursorRenderer(RawRenderer rawRenderer) {
        this.rawRenderer = rawRenderer;
    }

    @Override
    public void render(InputStream in, ImageBuffer destination, Rectangle rectangle) throws VncException {
        try {
            rawRenderer.render(in, destination, 0, 0, rectangle.getWidth(), rectangle.getHeight());

            byte[] bitmask = new byte[((rectangle.getWidth() + 7) / 8) * rectangle.getHeight()];
            new DataInputStream(in).readFully(bitmask);

            int x = 0;
            int y = 0;

            for (byte b : bitmask) {
                for (int i = 7; i >= 0; i--) {
                    boolean visible = bitAt(b, i);
                    if (!visible) {
                        destination.set(x, y,0);
                    }
                    if (++x == rectangle.getWidth()) {
                        x = 0;
                        y++;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }
}
