package com.shinyhut.vernacular.utils;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.spec.SecretKeySpec;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AesEaxOutputStream {

    private static final int MAX_MESSAGE_SIZE = 0xFFFF;
    private static final int MAC_SIZE = 16;

    private final byte[] key;
    private final EAXBlockCipher cipher;
    private final OutputStream outputStream;
    private BigInteger nonce = BigInteger.ZERO;

    public AesEaxOutputStream(byte[] key, OutputStream outputStream) {
        this.key = key;
        cipher = new EAXBlockCipher(new AESLightEngine());
        this.outputStream = outputStream;
    }

    public void write(byte[] b) throws IOException {
        // The message length field only has 2 bytes, so larger data has to be split up into multiple messages
        for (int offset = 0; offset < b.length; offset += MAX_MESSAGE_SIZE) {
            writeMessage(b, offset, Math.min(MAX_MESSAGE_SIZE, b.length - offset));
        }
    }

    private void writeMessage(byte[] b, int offset, int size) throws IOException {
        var nonceBytes = ByteUtils.bigIntToBytes(nonce,16, true);
        nonce = nonce.add(BigInteger.ONE);
        cipher.init(true, new AEADParameters(new KeyParameter(key), 16 * 8, nonceBytes));

        var lengthBytes = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(size).array(), 2,4);
        var out = new byte[size + MAC_SIZE];
        cipher.processAADBytes(lengthBytes, 0, 2);
        var length = cipher.processBytes(b, offset, size, out, 0);
        try {
            length += cipher.doFinal(out, length);
        } catch (InvalidCipherTextException e) {
            throw new IOException("Cipher failed", e);
        }
        outputStream.write(lengthBytes);
        outputStream.write(out,0, length);
        outputStream.flush();
    }
}
