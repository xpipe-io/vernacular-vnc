package com.shinyhut.vernacular.protocol.auth;

import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.SecurityTypeFailedException;
import com.shinyhut.vernacular.protocol.messages.SecurityResult;
import com.shinyhut.vernacular.utils.AesEaxInputStream;
import com.shinyhut.vernacular.utils.AesEaxOutputStream;
import com.shinyhut.vernacular.utils.ByteUtils;
import lombok.SneakyThrows;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import static com.shinyhut.vernacular.protocol.messages.SecurityType.RA2NE;

public class RsaAesAuthenticationHandler implements SecurityHandler {

    private static final int MIN_KEY_LENGTH = 1024;
    private static final int MAX_KEY_LENGTH = 8192;
    private final int keySize;
    private final MessageDigest digest;
    private int subtype;
    private PrivateKey clientKey;
    private PublicKey clientPublicKey;
    private PublicKey serverKey;
    private int serverKeyLength;
    private byte[] serverKeyN;
    private byte[] serverKeyE;
    private int clientKeyLength;
    private byte[] clientKeyN;
    private byte[] clientKeyE;
    private byte[] serverRandom;
    private byte[] clientRandom;
    private InputStream rawInput;
    private OutputStream rawOutput;
    private AesEaxInputStream encryptedInput;
    private AesEaxOutputStream encryptedOutput;
    private VncSession session;

    @SneakyThrows
    public RsaAesAuthenticationHandler(int keySize) {
        this.keySize = keySize;
        this.digest = MessageDigest.getInstance(keySize == 128 ? "SHA-1" : "SHA-256");
    }

    @Override
    public SecurityResult authenticate(VncSession session) throws IOException {
        this.session = session;
        rawInput = session.getInputStream();
        rawOutput = session.getOutputStream();

        if (!session.getProtocolVersion().equals(3, 3)) {
            requestAuthentication();
        }

        readPublicKey();
        verifyServer();
        writePublicKey();
        writeRandom();

        readRandom();
        setCipher();
        writeHash();

        readHash();
        readSubtype();
        writeCredentials();

        return SecurityResult.decode(rawInput, session.getProtocolVersion());
    }

    private void requestAuthentication() throws IOException {
        rawOutput.write(RA2NE.getCode());
    }

    @SneakyThrows
    private void readPublicKey() {
        var data = new DataInputStream(rawInput);
        serverKeyLength = data.readInt();
        if (serverKeyLength < MIN_KEY_LENGTH) {
            throw new SecurityTypeFailedException("Server key is too short");
        }
        if (serverKeyLength > MAX_KEY_LENGTH) {
            throw new SecurityTypeFailedException("Server key is too long");
        }
        var size = (serverKeyLength + 7) / 8;
        serverKeyN = new byte[size];
        serverKeyE = new byte[size];
        data.readFully(serverKeyN);
        data.readFully(serverKeyE);
        var modulus = new BigInteger(1, serverKeyN);
        var publicExponent = new BigInteger(1, serverKeyE);
        var spec = new RSAPublicKeySpec(modulus, publicExponent);
        try {
            var factory = KeyFactory.getInstance("RSA");
            serverKey = factory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new SecurityTypeFailedException("Server key is invalid");
        }
    }

    @SneakyThrows
    private void verifyServer() {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        var length = new byte[4];
        length[0] = (byte) ((serverKeyLength & 0xff000000) >> 24);
        length[1] = (byte) ((serverKeyLength & 0xff0000) >> 16);
        length[2] = (byte) ((serverKeyLength & 0xff00) >> 8);
        length[3] = (byte) (serverKeyLength & 0xff);
        digest.update(length);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        var f = digest.digest();
        //TODO: verify digest with format string %02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x
    }

    @SneakyThrows
    private void writePublicKey() {
        var data = new DataOutputStream(rawOutput);
        clientKeyLength = serverKeyLength;
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(clientKeyLength);
        KeyPair kp = kpg.generateKeyPair();
        clientKey = kp.getPrivate();
        clientPublicKey = kp.getPublic();
        var rsaKey = (RSAPublicKey) clientPublicKey;
        var modulus = rsaKey.getModulus();
        var publicExponent = rsaKey.getPublicExponent();

        clientKeyN = ByteUtils.bigIntToBytes(modulus, (clientKeyLength + 7) / 8);
        clientKeyE = ByteUtils.bigIntToBytes(publicExponent, (clientKeyLength + 7) / 8);
        data.writeInt(clientKeyLength);
        data.write(clientKeyN);
        data.write(clientKeyE);
    }

    @SneakyThrows
    private void writeRandom() {
        var data = new DataOutputStream(rawOutput);
        var sr = new SecureRandom();
        clientRandom = new byte[keySize / 8];
        sr.nextBytes(clientRandom);
        byte[] encrypted;
        try {
            var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, serverKey);
            encrypted = cipher.doFinal(clientRandom);
        } catch (InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new SecurityTypeFailedException("Failed to encrypt random");
        }
        data.writeShort(encrypted.length);
        data.write(encrypted);
    }

    @SneakyThrows
    private void readRandom() {
        var data = new DataInputStream(rawInput);
        var size = data.readShort();
        if (size != clientKeyN.length) {
            throw new SecurityTypeFailedException("Client key length doesn't match");
        }
        var buffer = new byte[size];
        data.readFully(buffer);
        try {
            var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, clientKey);
            serverRandom = cipher.doFinal(buffer);
        } catch (InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new SecurityTypeFailedException("Failed to decrypt server random");
        }
        if (serverRandom.length != keySize / 8) {
            throw new SecurityTypeFailedException("Server random length doesn't match");
        }
    }

    @SneakyThrows
    private void setCipher() {
        digest.update(clientRandom);
        digest.update(serverRandom);
        var key = Arrays.copyOfRange(digest.digest(), 0, keySize / 8);
        encryptedInput = new AesEaxInputStream(key, rawInput);
        digest.reset();
        digest.update(serverRandom);
        digest.update(clientRandom);
        key = Arrays.copyOfRange(digest.digest(), 0, keySize / 8);
        encryptedOutput = new AesEaxOutputStream(key, rawOutput);
    }

    @SneakyThrows
    private void writeHash() {
       var lenServerKey = new byte[]{
                (byte) ((serverKeyLength & 0xff000000) >> 24),
                (byte) ((serverKeyLength & 0xff0000) >> 16),
                (byte) ((serverKeyLength & 0xff00) >> 8),
                (byte) (serverKeyLength & 0xff)
        };
        var lenClientKey = new byte[]{
                (byte) ((clientKeyLength & 0xff000000) >> 24),
                (byte) ((clientKeyLength & 0xff0000) >> 16),
                (byte) ((clientKeyLength & 0xff00) >> 8),
                (byte) (clientKeyLength & 0xff)
        };
        digest.update(lenClientKey);
        digest.update(clientKeyN);
        digest.update(clientKeyE);
        digest.update(lenServerKey);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        var hash = digest.digest();
        encryptedOutput.write(hash);
    }

    @SneakyThrows
    void readHash() {
        var lenServerKey = new byte[]{
                (byte) ((serverKeyLength & 0xff000000) >> 24),
                (byte) ((serverKeyLength & 0xff0000) >> 16),
                (byte) ((serverKeyLength & 0xff00) >> 8),
                (byte) (serverKeyLength & 0xff)
        };
        var lenClientKey = new byte[]{
                (byte) ((clientKeyLength & 0xff000000) >> 24),
                (byte) ((clientKeyLength & 0xff0000) >> 16),
                (byte) ((clientKeyLength & 0xff00) >> 8),
                (byte) (clientKeyLength & 0xff)
        };
        digest.update(lenServerKey);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        digest.update(lenClientKey);
        digest.update(clientKeyN);
        digest.update(clientKeyE);
        var computedHash = digest.digest();
        var receivedHash = encryptedInput.read();
        if (!Arrays.equals(receivedHash, computedHash)) {
            throw new SecurityTypeFailedException("Hash doesn't match");
        }
    }

    @SneakyThrows
    private void readSubtype() {
        subtype = encryptedInput.read()[0];
        if (subtype != 1 && subtype != 2) {
            throw new SecurityTypeFailedException("Unknown RSA-AES authentication subtype");
        }
    }

    @SneakyThrows
    private void writeCredentials() {
        var pw = session.getConfig().getPasswordSupplier().get().getBytes(StandardCharsets.UTF_8);
        var buf = ByteBuffer.allocate(1 + 1 + pw.length);
        buf.put((byte) 0);
        buf.put((byte) pw.length);
        buf.put(pw);
        encryptedOutput.write(buf.array());
    }
}
