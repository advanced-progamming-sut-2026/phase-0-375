package model.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String password) {
       return  null;
    }

    public static boolean verify(String password, String storedHash) {
        return false;
    }


    private static String bytesToHex(byte[] bytes) {
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return false;
    }
}