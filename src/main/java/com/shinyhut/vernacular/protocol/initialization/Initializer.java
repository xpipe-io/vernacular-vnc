package com.shinyhut.vernacular.protocol.initialization;

import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import com.shinyhut.vernacular.protocol.messages.ClientInit;
import com.shinyhut.vernacular.protocol.messages.Encoding;
import com.shinyhut.vernacular.protocol.messages.PixelFormat;
import com.shinyhut.vernacular.protocol.messages.ServerInit;
import com.shinyhut.vernacular.protocol.messages.SetEncodings;
import com.shinyhut.vernacular.protocol.messages.SetPixelFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.shinyhut.vernacular.protocol.messages.Encoding.*;

public class Initializer {

    public void initialise(VncSession session) throws IOException {
        var encoder = session.getMessageEncoder();
        var decoder = session.getMessageDecoder();

        ClientInit clientInit = new ClientInit(session.getConfig().isShared());
        encoder.encode(clientInit);

        ServerInit serverInit = ServerInit.decode(decoder.getInputStream());
        session.setServerInit(serverInit);
        session.setFramebufferWidth(serverInit.getFramebufferWidth());
        session.setFramebufferHeight(serverInit.getFramebufferHeight());

        VernacularConfig config = session.getConfig();
        ColorDepth colorDepth = config.getColorDepth();

        PixelFormat pixelFormat = new PixelFormat(
                colorDepth.getBitsPerPixel(),
                colorDepth.getDepth(),
                true,
                colorDepth.isTrueColor(),
                colorDepth.getRedMax(),
                colorDepth.getGreenMax(),
                colorDepth.getBlueMax(),
                colorDepth.getRedShift(),
                colorDepth.getGreenShift(),
                colorDepth.getBlueShift());

        SetPixelFormat setPixelFormat = new SetPixelFormat(pixelFormat);

        List<Encoding> encodings = new ArrayList<>();

        if (config.isEnableZLibEncoding()) {
            encodings.add(ZLIB);
        }

        if (config.isEnableHextileEncoding()) {
            encodings.add(HEXTILE);
        }

        if (config.isEnableRreEncoding()) {
            encodings.add(RRE);
        }

        if (config.isEnableCopyrectEncoding()) {
            encodings.add(COPYRECT);
        }

        if (config.isEnableExtendedClipboard()) {
            encodings.add(EXTENDED_CLIPBOARD);
        }

        encodings.add(RAW);
        encodings.add(DESKTOP_SIZE);

        if (config.isUseLocalMousePointer()) {
            encodings.add(CURSOR);
        }

        SetEncodings setEncodings = new SetEncodings(encodings);

        encoder.encode(setPixelFormat);
        encoder.encode(setEncodings);

        session.setPixelFormat(pixelFormat);
    }

}
