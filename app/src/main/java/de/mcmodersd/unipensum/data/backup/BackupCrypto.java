package de.mcmodersd.unipensum.data.backup;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Protects the data of a backup with a password: AES-256 in GCM mode, the key derived from the password
 * with PBKDF2 (HMAC-SHA-256) and a random salt. Everything is in the JDK and in Android, so there is no
 * library to carry. Plain Java on purpose, so the unit tests can run it.
 * <p>
 * GCM both encrypts and authenticates. The manifest goes in as additional authenticated data, so changing
 * it (the version, the iteration count) makes opening fail just like a wrong password.
 */
final class BackupCrypto {

    static final String CIPHER_NAME = "AES-256-GCM";
    static final String KDF_NAME = "PBKDF2WithHmacSHA256";

    /** Deliberately slow: this is what makes guessing a password expensive. */
    static final int ITERATIONS = 600_000;
    /** Bounds for files from elsewhere: weak values are refused, absurd ones would freeze the app. */
    static final int MIN_ITERATIONS = 10_000;
    static final int MAX_ITERATIONS = 2_000_000;

    static final int SALT_BYTES = 16;
    static final int IV_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private BackupCrypto() {
    }

    /** The values that, together with the password, make the key; stored in the manifest. */
    record Params(int iterations, byte[] salt, byte[] iv) {
    }

    /** Fresh salt and nonce. A nonce is never used twice with one key; every file has its own salt. */
    static Params newParams(int iterations) {
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);
        return new Params(iterations, salt, iv);
    }

    /** @param aad data that is not encrypted but must be unchanged when the result is opened */
    static byte[] encrypt(char[] password, Params params, byte[] aad, byte[] plain) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(password, params), new GCMParameterSpec(TAG_BITS, params.iv()));
        cipher.updateAAD(aad);
        return cipher.doFinal(plain);
    }

    /**
     * @throws javax.crypto.AEADBadTagException if the password is wrong or the data or {@code aad} was changed
     */
    static byte[] decrypt(char[] password, Params params, byte[] aad, byte[] sealed) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(password, params), new GCMParameterSpec(TAG_BITS, params.iv()));
        cipher.updateAAD(aad);
        return cipher.doFinal(sealed);
    }

    private static SecretKey key(char[] password, Params params) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, params.salt(), params.iterations(), KEY_BITS);
        try {
            byte[] bytes = SecretKeyFactory.getInstance(KDF_NAME).generateSecret(spec).getEncoded();
            return new SecretKeySpec(bytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }
}
