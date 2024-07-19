package com.shinyhut.vernacular.protocol.handshaking;

import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.NoSupportedSecurityTypesException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.auth.*;
import com.shinyhut.vernacular.protocol.messages.SecurityType;
import com.shinyhut.vernacular.protocol.messages.ServerSecurityType;
import com.shinyhut.vernacular.protocol.messages.ServerSecurityTypes;

import java.io.IOException;
import java.util.List;

import static com.shinyhut.vernacular.protocol.messages.SecurityType.*;
import static java.util.Collections.singletonList;

public class SecurityTypeNegotiator {

    public SecurityHandler negotiate(VncSession session) throws IOException, VncException {
        if (session.getProtocolVersion().equals(3, 3)) {
            ServerSecurityType serverSecurityType = ServerSecurityType.decode(session.getInputStream());
            return resolve(singletonList(serverSecurityType.getSecurityType()));
        } else {
            ServerSecurityTypes serverSecurityTypes = ServerSecurityTypes.decode(session.getInputStream());
            return resolve(serverSecurityTypes.getSecurityTypes());
        }
    }

    private static SecurityHandler resolve(List<SecurityType> securityTypes) throws  VncException {
        if (securityTypes.contains(NONE)) {
            return new NoSecurityHandler();
        } else if (securityTypes.contains(VNC)) {
            return new VncAuthenticationHandler();
        } else if (securityTypes.contains(MS_LOGON_2)) {
            return new MsLogon2AuthenticationHandler();
        } else if (securityTypes.contains(RA2NE)) {
            return RsaAesAuthenticationHandler.RA2(RA2NE.getCode());
        } else if (securityTypes.contains(RA2NE_256)) {
            return RsaAesAuthenticationHandler.RA2_256(RA2NE_256.getCode());
        } else {
            throw new NoSupportedSecurityTypesException();
        }
    }

}
