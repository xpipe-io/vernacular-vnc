package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ImageBuffer;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.io.InputStream;

public interface Renderer {
    void render(InputStream in, ImageBuffer destination, Rectangle rectangle) throws VncException;
}
