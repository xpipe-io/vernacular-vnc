package com.shinyhut.vernacular.client;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static java.util.Collections.synchronizedList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.IntStream.range;

public class ClientEventHandler {

    private final VncSession session;
    private final Consumer<VncException> errorHandler;
    private final List<Boolean> buttons = synchronizedList(new ArrayList<>());
    private final ReentrantLock outputLock = new ReentrantLock(true);

    private volatile boolean running;
    private Thread framebufferUpdateLoop;

    private int mouseX;
    private int mouseY;

    // Monotonic System.nanoTime() value, unaffected by wall clock changes. Null if no update has been requested yet
    private Long lastFramebufferUpdateRequestTime;

    ClientEventHandler(VncSession session, Consumer<VncException> errorHandler) {
        this.session = session;
        this.errorHandler = errorHandler;
        range(0, 8).forEach(i -> buttons.add(false));
    }

    void start() {
        running = true;

        framebufferUpdateLoop = new Thread(() -> {
            try {
                boolean incremental = false;
                while (running) {
                    if (timeForFramebufferUpdate()) {
                        requestFramebufferUpdate(incremental);
                        incremental = true;
                        session.waitForFramebufferUpdate();
                    } else {
                        waitUntilFramebufferUpdateTime();
                    }
                }
            } catch (IOException | RuntimeException e) {
                if (running) {
                    errorHandler.accept(new UnexpectedVncException(e));
                }
            } catch (InterruptedException ignored) {
            } finally {
                running = false;
            }
        });

        framebufferUpdateLoop.start();
    }

    void stop() {
        running = false;
        try {
            // stop() can be called from the update loop itself via the error handler
            if (framebufferUpdateLoop != null && framebufferUpdateLoop != Thread.currentThread()) {
                framebufferUpdateLoop.interrupt();
                framebufferUpdateLoop.join(1000);
            }
        } catch (InterruptedException ignored) {
        }
    }

    void updateMouseButton(int button, boolean pressed) throws IOException {
        buttons.set(button - 1, pressed);
        updateMouseStatus();
    }

    void moveMouse(int mouseX, int mouseY) throws IOException {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        updateMouseStatus();
    }

    void updateKey(int keySym, boolean pressed) throws IOException {
        KeyEvent message = new KeyEvent(keySym, pressed);
        sendMessage(message);
    }

    void sendClientCutTextCaps() throws IOException {
        ClientCutTextCaps clientCutTextCaps = new ClientCutTextCaps(session.getConfig().getMaxSizePerFormat());
        sendMessage(clientCutTextCaps);
    }

    void copyText(String text) throws IOException {
        if (session.getConfig().isEnableExtendedClipboard()) {
            sendClientCutTextCaps();

            ClientCutTextExtendedClipboard clientCutTextExtendedClipboard = new ClientCutTextExtendedClipboard(text);
            sendMessage(clientCutTextExtendedClipboard);
        } else {
            ClientCutText message = new ClientCutText(text);
            sendMessage(message);
        }
    }

    void resize(int width, int height) throws IOException {
        var screens = List.of(new Screen(0, 0, 0, width, height));
        var message = new SetDesktopSize(width, height, screens);
        sendMessage(message);
    }

    private void updateMouseStatus() throws IOException {
        PointerEvent message = new PointerEvent(mouseX, mouseY, buttons);
        sendMessage(message);
    }

    private boolean timeForFramebufferUpdate() {
        long updateInterval = MILLISECONDS.toNanos(1000 / session.getConfig().getTargetFramesPerSecond());
        return lastFramebufferUpdateRequestTime == null ||
                System.nanoTime() - lastFramebufferUpdateRequestTime > updateInterval;
    }

    private void waitUntilFramebufferUpdateTime() throws InterruptedException {
        long updateInterval = MILLISECONDS.toNanos(1000 / session.getConfig().getTargetFramesPerSecond());
        long timeToWait = lastFramebufferUpdateRequestTime + updateInterval - System.nanoTime();
        if (timeToWait > 0L) {
            Thread.sleep(NANOSECONDS.toMillis(timeToWait), (int) (timeToWait % 1_000_000));
        }
    }

    private void requestFramebufferUpdate(boolean incremental) throws IOException {
        int width = session.getFramebufferWidth();
        int height = session.getFramebufferHeight();
        FramebufferUpdateRequest updateRequest = new FramebufferUpdateRequest(incremental, 0, 0, width, height);
        sendMessage(updateRequest);
        lastFramebufferUpdateRequestTime = System.nanoTime();
    }

    private void sendMessage(Encodable message) throws IOException {
        outputLock.lock();
        try {
            session.getMessageEncoder().encode(message);
        } finally {
            outputLock.unlock();
        }
    }
}
