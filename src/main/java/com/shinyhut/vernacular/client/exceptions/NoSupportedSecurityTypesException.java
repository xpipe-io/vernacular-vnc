package com.shinyhut.vernacular.client.exceptions;

import com.shinyhut.vernacular.protocol.messages.SecurityType;
import com.shinyhut.vernacular.protocol.messages.ServerSecurityType;
import com.shinyhut.vernacular.protocol.messages.ServerSecurityTypes;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class NoSupportedSecurityTypesException extends VncException {

    private final List<Integer> available;
    private final List<SecurityType> supported;

    public NoSupportedSecurityTypesException(List<Integer> available, List<SecurityType> supported) {
        super("The server does not support any VNC security types supported by this client." +
                      "\nAvailable: " + available.stream().map(Object::toString).collect(Collectors.joining(", ")) + "\nSupported:\n" + formatSupported(supported));
        this.available = available;
        this.supported = supported;
    }

    private static String formatSupported(List<SecurityType> supported) {
        return supported.stream().map(securityType -> "- " + securityType.getName() + " (" + securityType.getCode() + ")").collect(Collectors.joining("\n"));
    }
}
