package de.mcmodersd.unipensum.data.backup;

import static de.mcmodersd.unipensum.data.backup.BackupCrypto.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;

import javax.crypto.AEADBadTagException;

@SuppressWarnings("SpellCheckingInspection")
public class BackupCryptoTest {

    /** Far fewer than the real ones, which would only make the tests slow. */
    private static final int ITERATIONS = MIN_ITERATIONS;

    private static final byte[] PLAIN = "{\"lecturers\":[{\"id\":1,\"lastName\":\"Weber\"}]}".getBytes(UTF_8);
    private static final byte[] AAD = "{\"schema\":2}".getBytes(UTF_8);

    @Test
    public void whatIsEncrypted_comesBackWithTheSamePassword() throws Exception {
        var params = BackupCrypto.newParams(ITERATIONS);

        var sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        assertFalse(Arrays.equals(PLAIN, sealed));
        assertArrayEquals(PLAIN, BackupCrypto.decrypt("correct horse".toCharArray(), params, AAD, sealed));
    }

    @Test
    public void passwordsWithUmlautsAndEmoji_work() throws Exception {
        var params = BackupCrypto.newParams(ITERATIONS);
        var password = "Stundenplan-üß-😀".toCharArray();

        var sealed = BackupCrypto.encrypt(password.clone(), params, AAD, PLAIN);

        assertArrayEquals(PLAIN, BackupCrypto.decrypt(password.clone(), params, AAD, sealed));
    }

    @Test
    public void aWrongPassword_isRefused() throws Exception {
        var params = BackupCrypto.newParams(ITERATIONS);
        var sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        assertThrows(
                AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct hors".toCharArray(), params, AAD, sealed)
        );
        assertThrows(
                AEADBadTagException.class,
                () -> BackupCrypto.decrypt("Correct horse".toCharArray(), params, AAD, sealed)
        );
    }

    @Test
    public void changedAdditionalData_isRefused() throws Exception {
        var params = BackupCrypto.newParams(ITERATIONS);
        var sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        var otherManifest = "{\"schema\":3}".getBytes(UTF_8);

        assertThrows(
                AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct horse".toCharArray(), params, otherManifest, sealed)
        );
    }

    @Test
    public void changedEncryptedData_isRefused() throws Exception {
        var params = BackupCrypto.newParams(ITERATIONS);
        var sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);
        sealed[sealed.length / 2] ^= 0x01;

        assertThrows(
                AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct horse".toCharArray(), params, AAD, sealed)
        );
    }

    @Test
    public void everyFileGetsItsOwnSaltAndNonce_soTheSameDataLooksDifferent() throws Exception {
        var first = BackupCrypto.newParams(ITERATIONS);
        var second = BackupCrypto.newParams(ITERATIONS);

        assertEquals(SALT_BYTES, first.salt().length);
        assertEquals(IV_BYTES, first.iv().length);
        assertFalse(Arrays.equals(first.salt(), second.salt()));
        assertFalse(Arrays.equals(first.iv(), second.iv()));
        assertFalse(
                Arrays.equals(
                        BackupCrypto.encrypt("pw-12345".toCharArray(), first, AAD, PLAIN),
                        BackupCrypto.encrypt("pw-12345".toCharArray(), second, AAD, PLAIN)
                )
        );
    }

    // A guard on constants: it fails as soon as one of them changes so that a file we write could not be read back.
    @SuppressWarnings("ConstantValue")
    @Test
    public void theRealIterationCountIsWithinTheBoundsAFileMayUse() {
        assertTrue(BackupCrypto.ITERATIONS >= MIN_ITERATIONS);
        assertTrue(BackupCrypto.ITERATIONS <= MAX_ITERATIONS);
    }
}