package com.shinyhut.vernacular.protocol.messages;

import lombok.Getter;

import java.util.Optional;

import static java.util.Arrays.stream;

@Getter
public enum SecurityType {

    NONE(1, "None"),
    VNC(2, "VncAuth"),
    RA2(5, "RSA-AES (RA2)"),
    RA2NE(6, "RSA-AES (RA2ne)"),
    MS_LOGON_2(113, "MsLogonII"),
    RA2_256(129, "RSA-AES-256 (RA2_256)"),
    RA2NE_256(130, "RSA-AES-256 (RA2ne_256)");

    private final int code;
    private final String name;

    SecurityType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Optional<SecurityType> resolve(int code) {
        return stream(values()).filter(s -> s.code == code).findFirst();
    }
}
