package com.shinyhut.vernacular.utils;

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

    private final byte[] key;
    private final EAXBlockCipher cipher;
    private final OutputStream outputStream;
    private BigInteger nonce = BigInteger.ZERO;

    public AesEaxOutputStream(byte[] key, OutputStream outputStream) {
        this.key = key;
        cipher = new EAXBlockCipher(new AESLightEngine());
        this.outputStream = outputStream;
    }

    public void write(byte[] b) throws Exception {
        var nonceBytes = ByteUtils.bigIntToBytes(nonce,16);
        nonce = nonce.add(BigInteger.ONE);
        cipher.init(true, new AEADParameters(new KeyParameter(key), 16 * 8, nonceBytes));

        var lengthBytes = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(b.length).array(), 2,4);
        var out = new byte[1024];
        cipher.processAADBytes(lengthBytes, 0, 2);
        var length = cipher.processBytes(b,0,b.length, out, 0);
        length += cipher.doFinal(out, length);
        outputStream.write(lengthBytes);
        outputStream.write(out,0, length);
        outputStream.flush();
    }
}
