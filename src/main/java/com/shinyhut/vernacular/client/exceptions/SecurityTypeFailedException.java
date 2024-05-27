package com.shinyhut.vernacular.client.exceptions;

public class SecurityTypeFailedException extends VncException {

    public SecurityTypeFailedException(String info) {
        super("Security authentication failed: " + info);
    }
}
