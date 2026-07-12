package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Tiện ích PBKDF2; mật khẩu gốc không bao giờ được ghi xuống CSV. */
public final class PasswordHasher {
    public static final String DEFAULT_PASSWORD = "Flash@123";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static PasswordData hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        return new PasswordData(hash(password, salt), Base64.getEncoder().encodeToString(salt));
    }

    public static boolean verify(String password, String expectedHash, String saltBase64) {
        if (password == null || expectedHash == null || expectedHash.isEmpty()
                || saltBase64 == null || saltBase64.isEmpty()) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] expected = Base64.getDecoder().decode(expectedHash);
            byte[] actual = Base64.getDecoder().decode(hash(password, salt));
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String hash(String password, byte[] salt) {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM khong ho tro PBKDF2WithHmacSHA256", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }

    public static final class PasswordData {
        private final String hash;
        private final String salt;

        private PasswordData(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }

        public String getHash() { return hash; }
        public String getSalt() { return salt; }
    }
}
