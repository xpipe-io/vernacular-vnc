package com.shinyhut.vernacular.utils;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;

public class CryptoUtils {

    @SneakyThrows
    public static MessageDigest sha1() {
        return MessageDigest.getInstance("SHA-1");
    }


    @SneakyThrows
    public static MessageDigest sha256() {
        return MessageDigest.getInstance("SHA-256");
    }

    @SneakyThrows
    public static KeyPairGenerator rsaKeyPairGenerator() {
        return KeyPairGenerator.getInstance("RSA");
    }

    @SneakyThrows
    public static KeyFactory rsaKeyFactory() {
        return KeyFactory.getInstance("RSA");
    }

    @SneakyThrows
    public static Cipher rsaEcbPkcs1PaddingCipher() {
        return Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }
}
