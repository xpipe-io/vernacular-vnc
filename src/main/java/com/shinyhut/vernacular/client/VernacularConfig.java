package com.shinyhut.vernacular.client;

import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import com.shinyhut.vernacular.protocol.messages.MessageHeaderFlags;
import com.shinyhut.vernacular.client.rendering.ImageBuffer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_8_INDEXED;

public class VernacularConfig {

    private Supplier<String> usernameSupplier;
    private Supplier<String> passwordSupplier;
    private Consumer<VncException> errorListener;
    private Consumer<ImageBuffer> screenUpdateListener;
    private Consumer<Void> bellListener;
    private Consumer<String> remoteClipboardListener;
    private MousePointerUpdateListener mousePointerUpdateListener;
    private boolean shared = true;
    private int targetFramesPerSecond = 30;
    private boolean useLocalMousePointer = false;
    private boolean enableCopyrectEncoding = true;
    private boolean enableExtendedClipboard = true;
    private boolean enableRreEncoding = true;
    private boolean enableHextileEncoding = true;
    private boolean enableZLibEncoding = false;
    private final Map<MessageHeaderFlags, Integer> maxSizePerFormat = new EnumMap<>(MessageHeaderFlags.class);

    public Supplier<String> getUsernameSupplier() {
        return usernameSupplier;
    }

    /**
     * Specifies a Supplier which will be called to find the VNC username if the remote host uses an authentication
     * scheme that requires one.
     *
     * @param usernameSupplier A Supplier which when invoked will return the user's VNC username
     */
    public void setUsernameSupplier(Supplier<String> usernameSupplier) {
        this.usernameSupplier = usernameSupplier;
    }

    public Supplier<String> getPasswordSupplier() {
        return passwordSupplier;
    }

    /**
     * Specifies a Supplier which will be called to find the VNC password if the remote host requires authentication.
     *
     * @param passwordSupplier A Supplier which when invoked will return the user's VNC password
     */
    public void setPasswordSupplier(Supplier<String> passwordSupplier) {
        this.passwordSupplier = passwordSupplier;
    }

    public Consumer<VncException> getErrorListener() {
        return errorListener;
    }

    /**
     * Specifies a Consumer which will be passed any Exception which occurs during the VNC session.
     *
     * @param errorListener A Consumer which will receive any Exceptions which occur during the VNC session
     */
    public void setErrorListener(Consumer<VncException> errorListener) {
        this.errorListener = errorListener;
    }

    public Consumer<ImageBuffer> getScreenUpdateListener() {
        return screenUpdateListener;
    }

    /**
     * Specifies a Consumer which will be passed an Image representing the remote server's desktop every time
     * we receive a screen update.
     *
     * @param screenUpdateListener A Consumer which will receive Images representing the updated remote desktop
     */
    public void setScreenUpdateListener(Consumer<ImageBuffer> screenUpdateListener) {
        this.screenUpdateListener = screenUpdateListener;
    }

    public static interface MousePointerUpdateListener {

        void update(int x, int y, ImageBuffer imageBuffer);
    }

    public MousePointerUpdateListener getMousePointerUpdateListener() {
        return mousePointerUpdateListener;
    }

    /**
     * Specifies a Consumer which will be passed an Image representing the remote server's mouse pointer image, and a
     * Point representing its hotspot (i.e. the point within the cursor that interacts with other elements on the
     * screen).
     *
     * @param mousePointerUpdateListener A Consumer which will receive Images representing the updated cursor shape
     */
    public void setMousePointerUpdateListener(MousePointerUpdateListener mousePointerUpdateListener) {
        this.mousePointerUpdateListener = mousePointerUpdateListener;
    }

    public Consumer<String> getRemoteClipboardListener() {
        return remoteClipboardListener;
    }

    /**
     * Specifies a Consumer which will be invoked when the sever wants to store text in the clipboard
     *
     * @param remoteClipboardListener A Consumer which will be invoked when the sever wants to store text in the clipboard
     */
    public void setRemoteClipboardListener(Consumer<String> remoteClipboardListener) {
        this.remoteClipboardListener = remoteClipboardListener;
    }

    public Consumer<Void> getBellListener() {
        return bellListener;
    }

    /**
     * Specifies a Consumer which will be invoked when the server triggers an alert sound
     *
     * @param bellListener A Consumer which will be notified when the server triggers an alert sound
     */
    public void setBellListener(Consumer<Void> bellListener) {
        this.bellListener = bellListener;
    }

    public boolean isShared() {
        return shared;
    }

    /**
     * Specifies whether we should request 'shared' access to the remote server.
     * <p>
     * If this is set to false, most servers will disconnect any other clients as soon as we connect.
     * <p>
     * Default: true
     *
     * @param shared Should we request shared access to the remote server?
     */
    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public int getTargetFramesPerSecond() {
        return targetFramesPerSecond;
    }

    /**
     * Sets the target number of frames per second we wish to receive from the remote server.
     * <p>
     * Note that this is the maximum number of framebuffer updates we will request per second. The server does not have
     * to honour our requests. The higher the number of frames per second, the more bandwidth we will consume.
     * <p>
     * Default: 30
     *
     * @param targetFramesPerSecond The number of frames per second we want to receive from the remote server
     */
    public void setTargetFramesPerSecond(int targetFramesPerSecond) {
        this.targetFramesPerSecond = targetFramesPerSecond;
    }

    /**
     * Indicate to the server that the client can draw the mouse pointer locally. The server should not include the
     * mouse pointer in framebuffer updates, and it should send separate notifications when the mouse pointer image
     * changes
     *
     * @see #setMousePointerUpdateListener(MousePointerUpdateListener)
     * @param useLocalMousePointer enable or disable client side mouse pointer rendering
     */
    public void setUseLocalMousePointer(boolean useLocalMousePointer) {
        this.useLocalMousePointer = useLocalMousePointer;
    }

    public boolean isUseLocalMousePointer() {
        return useLocalMousePointer;
    }

    public boolean isEnableCopyrectEncoding() {
        return enableCopyrectEncoding;
    }

    public boolean isEnableExtendedClipboard() {
        return enableExtendedClipboard;
    }

    /**
     * Enable or disable the Extended clipboard encoding support
     * @param enableExtendedClipboard enable or disable the Extended clipboard encoding support
     */
    public void setEnableExtendedClipboard(boolean enableExtendedClipboard) {
        this.enableExtendedClipboard = enableExtendedClipboard;
    }

    /**
     * Enable or disable the COPYRECT video encoding
     * @param enableCopyrectEncoding enable or disable the COPYRECT video encoding
     */
    public void setEnableCopyrectEncoding(boolean enableCopyrectEncoding) {
        this.enableCopyrectEncoding = enableCopyrectEncoding;
    }

    public boolean isEnableRreEncoding() {
        return enableRreEncoding;
    }

    /**
     * Enable or disable the RRE video encoding
     * @param enableRreEncoding enable or disable the RRE video encoding
     */
    public void setEnableRreEncoding(boolean enableRreEncoding) {
        this.enableRreEncoding = enableRreEncoding;
    }

    public boolean isEnableHextileEncoding() {
        return enableHextileEncoding;
    }

    /**
     * Enable or disable the HEXTILE video encoding
     * @param enableHextileEncoding enable or disable the HEXTILE video encoding
     */
    public void setEnableHextileEncoding(boolean enableHextileEncoding) {
        this.enableHextileEncoding = enableHextileEncoding;
    }

    public boolean isEnableZLibEncoding() {
        return enableZLibEncoding;
    }

    /**
     * Enable or disable the ZLIB video encoding. This encoding is disabled by default because it is very expensive
     * in terms of CPU usage, but it may be useful in situations where you are bandwidth constrained.
     * @param enableZLibEncoding enable or disable the ZLIB video encoding
     */
    public void setEnableZLibEncoding(boolean enableZLibEncoding) {
        this.enableZLibEncoding = enableZLibEncoding;
    }

    public Map<MessageHeaderFlags, Integer> getMaxSizePerFormat() {
        return maxSizePerFormat;
    }
}
