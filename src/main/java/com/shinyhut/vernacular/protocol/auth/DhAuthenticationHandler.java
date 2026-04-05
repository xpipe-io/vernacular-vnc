package com.shinyhut.vernacular.protocol.auth;

import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.AuthenticationRequiredException;
import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.SecurityResult;
import org.bouncycastle.crypto.digests.MD5Digest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

import static com.shinyhut.vernacular.protocol.messages.SecurityType.DH;
import static com.shinyhut.vernacular.utils.ByteUtils.*;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class DhAuthenticationHandler implements SecurityHandler {

    private static final long DH_KEY_MAX_BITS = 31;
    private static final long MAX_DH_KEY_VALUE = 1L << DH_KEY_MAX_BITS;

    private final Random random;

    public DhAuthenticationHandler() throws VncException {
        try {
            this.random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new UnexpectedVncException(e);
        }
    }

    DhAuthenticationHandler(Random random) {
        this.random = random;
    }

    @Override
    public SecurityResult authenticate(VncSession session) throws VncException, IOException {
        Supplier<String> usernameSupplier = session.getConfig().getUsernameSupplier();
        Supplier<String> passwordSupplier = session.getConfig().getPasswordSupplier();

        if (usernameSupplier == null || passwordSupplier == null) {
            throw new AuthenticationRequiredException();
        }

        InputStream in = session.getInputStream();
        OutputStream out = session.getOutputStream();

        if (!session.getProtocolVersion().equals(3, 3)) {
            requestDhAuthentication(out);
        }

        try {
            DataInput dataInput = new DataInputStream(in);

            var generatorValue = dataInput.readUnsignedShort();
            var keySize = dataInput.readUnsignedShort();

            var modulusBytes = new byte[keySize];
            var serverPublicKeyBytes = new byte[keySize];
            dataInput.readFully(modulusBytes);
            dataInput.readFully(serverPublicKeyBytes);

            BigInteger generator = BigInteger.valueOf(generatorValue);
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger serverPublicKey = new BigInteger(1, serverPublicKeyBytes);
            BigInteger clientPrivateKey = BigInteger.valueOf(random.nextLong() % MAX_DH_KEY_VALUE);
            BigInteger clientPublicKey = generator.modPow(clientPrivateKey, modulus);
            BigInteger sharedKey = serverPublicKey.modPow(clientPrivateKey, modulus);

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bigIntToBytes(sharedKey, keySize, false));
            byte[] digest = md.digest();

            sendEncrypted(out, usernameSupplier.get(), 64, digest);
            sendEncrypted(out, passwordSupplier.get(), 64, digest);
            out.write(bigIntToBytes(clientPublicKey, keySize, false));
            return SecurityResult.decode(in, session.getProtocolVersion());
        } catch (GeneralSecurityException e) {
            throw new UnexpectedVncException(e);
        }
    }

    private void requestDhAuthentication(OutputStream out) throws IOException {
        out.write(DH.getCode());
    }

    public void sendEncrypted(OutputStream out, String value, int length, byte[] key) throws GeneralSecurityException, IOException {
        byte[] bytes = padRight(value.getBytes(UTF_8), length);
        byte[] encrypted = des(bytes, key);
        out.write(encrypted);
    }

    public byte[] des(byte[] data, byte[] key) throws GeneralSecurityException {
        Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, 0, key.length, "AES");
        aes.init(ENCRYPT_MODE, keySpec);
        return aes.doFinal(data);
    }
}
