package com.shinyhut.vernacular.utils;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AesEaxInputStream {

    private final byte[] key;
    private final EAXBlockCipher cipher;
    private final InputStream inputStream;
    private BigInteger nonce = BigInteger.ZERO;

    public AesEaxInputStream(byte[] key, InputStream inputStream) {
        this.key = key;
        cipher = new EAXBlockCipher(new AESLightEngine());
        this.inputStream = inputStream;
    }

    public byte[] read() throws IOException {
        var nonceBytes = ByteUtils.bigIntToBytes(nonce,16, true);
        nonce = nonce.add(BigInteger.ONE);
        cipher.init(false, new AEADParameters(new KeyParameter(key), 16 * 8, nonceBytes));

        var out = new byte[1024];
        var length = new DataInputStream(inputStream).readUnsignedShort();
        var lengthBytes = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(length).array(), 2, 4);
        var toRead = length + 16;
        var read = inputStream.readNBytes(toRead);
        cipher.processAADBytes(lengthBytes, 0, 2);
        var outIndex = cipher.processBytes(read, 0, toRead, out,0);
        try {
            cipher.doFinal(out, outIndex);
        } catch (InvalidCipherTextException e) {
            throw new IOException("Cipher failed", e);
        }
        return Arrays.copyOfRange(out, 0, length);
    }
}
