package com.shinyhut.vernacular.client.exceptions;

import static java.lang.String.format;

public class RealVncProtocolVersionException extends VncException {

    private final int serverMajor;
    private final int serverMinor;
    private final int maxMajor;
    private final int maxMinor;

    public RealVncProtocolVersionException(int serverMajor, int serverMinor, int minMajor, int minMinor) {
        super(format("The server uses the proprietary RealVNC protocol version %d.%d. To use VNC viewers other than RealVNC," +
                             " you can either try downgrading the server protocol version to %d.%d" +
                             " or use another open VNC server software.",
                serverMajor, serverMinor, minMajor, minMinor));
        this.serverMajor = serverMajor;
        this.serverMinor = serverMinor;
        this.maxMajor = minMajor;
        this.maxMinor = minMinor;
    }

    public int getServerMajor() {
        return serverMajor;
    }

    public int getServerMinor() {
        return serverMinor;
    }

    public int getMaxMajor() {
        return maxMajor;
    }

    public int getMaxMinor() {
        return maxMinor;
    }
}
