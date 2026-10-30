package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.InvalidMessageException;
import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ImageBuffer;
import com.shinyhut.vernacular.protocol.messages.PixelFormat;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZLibRenderer implements Renderer {

    private final RawRenderer rawRenderer;
    private final PixelFormat pixelFormat;
    private final Inflater inflater;

    public ZLibRenderer(RawRenderer rawRenderer, PixelFormat pixelFormat) {
        this.rawRenderer = rawRenderer;
        this.pixelFormat = pixelFormat;
        this.inflater = new Inflater();
    }

    @Override
    public void render(InputStream in, ImageBuffer destination, Rectangle rectangle) throws VncException {
        try {
            DataInput dataInput = new DataInputStream(in);
            int compressedLength = dataInput.readInt();
            if (compressedLength < 0) {
                throw new InvalidMessageException("ZLib");
            }

            // Reads in chunks, so an invalid large length does not allocate a large buffer upfront
            byte[] compressedData = in.readNBytes(compressedLength);
            if (compressedData.length != compressedLength) {
                throw new EOFException();
            }
            inflater.setInput(compressedData);

            // The decompressed data is never larger than the raw pixel data of the rectangle.
            // The buffer is not allocated upfront as the rectangle size is also sent by the server
            long expectedLength = (long) rectangle.getWidth() * rectangle.getHeight() * pixelFormat.getBytesPerPixel();

            // All input has to be consumed, as the zlib stream continues across rectangles
            int read;
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((read = inflater.inflate(buffer)) != 0) {
                if (baos.size() + read > expectedLength) {
                    throw new InvalidMessageException("ZLib");
                }
                baos.write(buffer, 0, read);
            }

            if (baos.size() != expectedLength) {
                throw new InvalidMessageException("ZLib");
            }

            byte[] decompressedData = baos.toByteArray();

            rawRenderer.render(
                    new ByteArrayInputStream(decompressedData),
                    destination,
                    rectangle.getX(),
                    rectangle.getY(),
                    rectangle.getWidth(),
                    rectangle.getHeight());

        } catch (IOException | DataFormatException e) {
            throw new UnexpectedVncException(e);
        }
    }
}
