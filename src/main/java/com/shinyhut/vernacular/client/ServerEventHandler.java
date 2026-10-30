package com.shinyhut.vernacular.client;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.UnknownMessageTypeException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.Framebuffer;
import com.shinyhut.vernacular.protocol.messages.Bell;
import com.shinyhut.vernacular.protocol.messages.FramebufferUpdate;
import com.shinyhut.vernacular.protocol.messages.ServerCutText;
import com.shinyhut.vernacular.protocol.messages.SetColorMapEntries;

import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.function.Consumer;

public class ServerEventHandler {

    private final VncSession session;
    private final Consumer<VncException> errorHandler;
    private final Framebuffer framebuffer;

    private volatile boolean running;
    private Thread eventLoop;

    ServerEventHandler(VncSession session, Consumer<VncException> errorHandler) {
        this.session = session;
        this.errorHandler = errorHandler;
        this.framebuffer = new Framebuffer(session);
    }

    void start() throws IOException {
        PushbackInputStream in = new PushbackInputStream(session.getMessageDecoder().getInputStream());

        running = true;

        eventLoop = new Thread(() -> {
            try {
                int messageType;
                while (running && ((messageType = in.read()) != -1)) {
                    in.unread(messageType);

                    switch (messageType) {
                        case 0x00:
                            FramebufferUpdate framebufferUpdate = FramebufferUpdate.decode(in);
                            framebuffer.processUpdate(framebufferUpdate);
                            break;
                        case 0x01:
                            SetColorMapEntries setColorMapEntries = SetColorMapEntries.decode(in);
                            framebuffer.updateColorMap(setColorMapEntries);
                            break;
                        case 0x02:
                            Bell.decode(in);
                            Consumer<Void> bellListener = session.getConfig().getBellListener();
                            if (bellListener != null) {
                                bellListener.accept(null);
                            }
                            break;
                        case 0x03:
                            ServerCutText cutText = ServerCutText.decode(in);
                            Consumer<String> cutTextListener = session.getConfig().getRemoteClipboardListener();
                            if (cutTextListener != null && !cutText.getText().isEmpty()) {
                                cutTextListener.accept(cutText.getText());
                            }
                            break;
                        default:
                            throw new UnknownMessageTypeException(messageType);
                    }
                }

                if (running) {
                    errorHandler.accept(new UnexpectedVncException(new EOFException("Connection closed by server")));
                }
            } catch (IOException | RuntimeException e) {
                if (running) {
                    errorHandler.accept(new UnexpectedVncException(e));
                }
            } catch (VncException e) {
                if (running) {
                    errorHandler.accept(e);
                }
            } finally {
                running = false;
            }
        });

        eventLoop.start();
    }

    void stop() {
        running = false;
        try {
            // stop() can be called from the event loop itself via the error handler
            if (eventLoop != null && eventLoop != Thread.currentThread()) {
                eventLoop.join(1000);
            }
        } catch (InterruptedException ignored) {
        }
    }

}
