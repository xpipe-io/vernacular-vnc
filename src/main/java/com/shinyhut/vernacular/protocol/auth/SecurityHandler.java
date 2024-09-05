package com.shinyhut.vernacular.protocol.auth;

import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.Decoder;
import com.shinyhut.vernacular.protocol.messages.Encoder;
import com.shinyhut.vernacular.protocol.messages.SecurityResult;

import java.io.IOException;

public interface SecurityHandler {

    SecurityResult authenticate(VncSession session) throws VncException, IOException;

    default Encoder getSessionEncoder(VncSession session) {
        return Encoder.raw(session.getOutputStream());
    }

    default Decoder getSessionDecoder(VncSession session) {
        return Decoder.raw(session.getInputStream());
    }
}
