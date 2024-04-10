package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ImageBuffer;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public class CopyRectRenderer implements Renderer {

    @Override
    public void render(InputStream in, ImageBuffer destination, Rectangle rectangle) throws VncException {
        try {
            DataInput dataInput = new DataInputStream(in);
            int srcX = dataInput.readUnsignedShort();
            int srcY = dataInput.readUnsignedShort();
            destination.duplicate(srcX, srcY, rectangle.getWidth(), rectangle.getHeight(), rectangle.getX(), rectangle.getY());
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }
}
