package com.shinyhut.vernacular.client.rendering;

import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.renderers.*;
import com.shinyhut.vernacular.protocol.messages.*;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.shinyhut.vernacular.protocol.messages.Encoding.*;

public class Framebuffer {

    private final VncSession session;
    private final Map<Long, ColorMapEntry> colorMap = new ConcurrentHashMap<>();
    private final Map<Encoding, Renderer> renderers = new ConcurrentHashMap<>();
    private final CursorRenderer cursorRenderer;

    private ImageBuffer frame;

    public Framebuffer(VncSession session) {
        PixelDecoder pixelDecoder = new PixelDecoder(colorMap);
        RawRenderer rawRenderer = new RawRenderer(pixelDecoder, session.getPixelFormat());
        renderers.put(RAW, rawRenderer);
        renderers.put(COPYRECT, new CopyRectRenderer());
        renderers.put(RRE, new RRERenderer(pixelDecoder, session.getPixelFormat()));
        renderers.put(HEXTILE, new HextileRenderer(rawRenderer, pixelDecoder, session.getPixelFormat()));
        renderers.put(ZLIB, new ZLibRenderer(rawRenderer));
        cursorRenderer = new CursorRenderer(rawRenderer);

        frame = new ImageBuffer(session.getFramebufferWidth(), session.getFramebufferHeight(), false);
        this.session = session;
    }

    public void processUpdate(FramebufferUpdate update) throws VncException {
        try {
            var in = session.getMessageDecoder().getInputStream();
            for (int i = 0; i < update.getNumberOfRectangles(); i++) {
                Rectangle rectangle = Rectangle.decode(in);
                if (rectangle.getEncoding() == DESKTOP_SIZE) {
                    resizeFramebuffer(rectangle);
                } else if (rectangle.getEncoding() == CURSOR) {
                    updateCursor(rectangle, in);
                } else if (rectangle.getEncoding() == EXTENDED_DESKTOP_SIZE) {
                    updateExtendedDesktopSize(rectangle, in);
                } else {
                    renderers.get(rectangle.getEncoding()).render(in, frame, rectangle);
                }
            }
            paint();
            session.framebufferUpdated();
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }

    private void paint() {
        Consumer<ImageBuffer> listener = session.getConfig().getScreenUpdateListener();
        if (listener != null) {
            listener.accept(frame);
        }
    }

    public void updateColorMap(SetColorMapEntries update) {
        for (int i = 0; i < update.getColors().size(); i++) {
            colorMap.put((long) i + update.getFirstColor(), update.getColors().get(i));
        }
    }

    private void resizeFramebuffer(Rectangle newSize) {
        int width = newSize.getWidth();
        int height = newSize.getHeight();
        session.setFramebufferWidth(width);
        session.setFramebufferHeight(height);
        frame = new ImageBuffer(width, height, false);

        VernacularConfig.ScreenResizeListener listener = session.getConfig().getScreenResizeListener();
        if (listener != null) {
            listener.update(width, height);
        }
    }

    private void updateCursor(Rectangle cursor, InputStream in) throws VncException {
        if (cursor.getWidth() > 0 && cursor.getHeight() > 0) {
            ImageBuffer cursorImage = new ImageBuffer(cursor.getWidth(), cursor.getHeight(), true);
            cursorRenderer.render(in, cursorImage, cursor);
            VernacularConfig.MousePointerUpdateListener listener = session.getConfig().getMousePointerUpdateListener();
            if (listener != null) {
                listener.update(cursor.getX(), cursor.getY(), cursorImage);
            }
        }
    }

    private void updateExtendedDesktopSize(Rectangle rect, InputStream in) throws VncException {
        try {
            // Read screen data but don't use it
            int screens = in.read();
            in.readNBytes(3 + (screens * 16));

            int width = rect.getWidth();
            int height = rect.getHeight();

            session.setFramebufferWidth(width);
            session.setFramebufferHeight(height);
            frame = new ImageBuffer(width, height, false);

            VernacularConfig.ScreenResizeListener listener = session.getConfig().getScreenResizeListener();
            if (listener != null) {
                listener.update(width, height);
            }
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }
}
