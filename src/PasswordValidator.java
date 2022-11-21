import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordValidator {
    private static final SecureRandom rng = new SecureRandom();
    private static final MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return a 30 byte long, securely random generated salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[30];

        synchronized (rng) {
            rng.nextBytes(salt);
        }

        return salt;
    }

    /**
     * Generate a hash for the given password combined with the salt.
     *
     * @return the generated hash as a byte array
     */
    public static byte[] generateHash(String password, byte[] salt) {
        byte[] hash;

        synchronized (digest) {
            digest.update(salt);
            hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            digest.reset();
        }

        return hash;
    }

    /**
     * This function compares the password to the underlying salt + password which generated the hash.
     * It can be used to validate a login password.
     *
     * @param password The input password to check equality for.
     * @param salt     The salt which was generated for the initial password.
     * @param hash     The hash calculated for the initial input string.
     * @return true if and only if the input password is the same as the initial password, otherwise false.
     */
    public static boolean isValid(String password, byte[] salt, byte[] hash) {
        byte[] passwordHash;

        synchronized (digest) {
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            passwordHash = digest.digest(salt);
            digest.reset();
        }

        return MessageDigest.isEqual(passwordHash, hash);
    }
}
