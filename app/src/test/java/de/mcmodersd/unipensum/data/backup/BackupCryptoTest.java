package de.mcmodersd.unipensum.data.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;

public class BackupCryptoTest {

    /** Far fewer than the real ones, which would only make the tests slow. */
    private static final int ITERATIONS = BackupCrypto.MIN_ITERATIONS;

    private static final byte[] PLAIN = "{\"lecturers\":[{\"id\":1,\"lastName\":\"Weber\"}]}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] AAD = "{\"schema\":2}".getBytes(StandardCharsets.UTF_8);

    @Test
    public void whatIsEncrypted_comesBackWithTheSamePassword() throws Exception {
        BackupCrypto.Params params = BackupCrypto.newParams(ITERATIONS);

        byte[] sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        assertFalse(Arrays.equals(PLAIN, sealed));
        assertArrayEquals(PLAIN, BackupCrypto.decrypt("correct horse".toCharArray(), params, AAD, sealed));
    }

    @Test
    public void passwordsWithUmlautsAndEmoji_work() throws Exception {
        BackupCrypto.Params params = BackupCrypto.newParams(ITERATIONS);
        char[] password = "Stundenplan-üß-😀".toCharArray();

        byte[] sealed = BackupCrypto.encrypt(password.clone(), params, AAD, PLAIN);

        assertArrayEquals(PLAIN, BackupCrypto.decrypt(password.clone(), params, AAD, sealed));
    }

    @Test
    public void aWrongPassword_isRefused() throws Exception {
        BackupCrypto.Params params = BackupCrypto.newParams(ITERATIONS);
        byte[] sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        assertThrows(AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct hors".toCharArray(), params, AAD, sealed));
        assertThrows(AEADBadTagException.class,
                () -> BackupCrypto.decrypt("Correct horse".toCharArray(), params, AAD, sealed));
    }

    @Test
    public void changedAdditionalData_isRefused() throws Exception {
        BackupCrypto.Params params = BackupCrypto.newParams(ITERATIONS);
        byte[] sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);

        byte[] otherManifest = "{\"schema\":3}".getBytes(StandardCharsets.UTF_8);

        assertThrows(AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct horse".toCharArray(), params, otherManifest, sealed));
    }

    @Test
    public void changedEncryptedData_isRefused() throws Exception {
        BackupCrypto.Params params = BackupCrypto.newParams(ITERATIONS);
        byte[] sealed = BackupCrypto.encrypt("correct horse".toCharArray(), params, AAD, PLAIN);
        sealed[sealed.length / 2] ^= 0x01;

        assertThrows(AEADBadTagException.class,
                () -> BackupCrypto.decrypt("correct horse".toCharArray(), params, AAD, sealed));
    }

    @Test
    public void everyFileGetsItsOwnSaltAndNonce_soTheSameDataLooksDifferent() throws Exception {
        BackupCrypto.Params first = BackupCrypto.newParams(ITERATIONS);
        BackupCrypto.Params second = BackupCrypto.newParams(ITERATIONS);

        assertEquals(BackupCrypto.SALT_BYTES, first.salt().length);
        assertEquals(BackupCrypto.IV_BYTES, first.iv().length);
        assertFalse(Arrays.equals(first.salt(), second.salt()));
        assertFalse(Arrays.equals(first.iv(), second.iv()));
        assertFalse(Arrays.equals(
                BackupCrypto.encrypt("pw-12345".toCharArray(), first, AAD, PLAIN),
                BackupCrypto.encrypt("pw-12345".toCharArray(), second, AAD, PLAIN)));
    }

    @Test
    public void theRealIterationCountIsWithinTheBoundsAFileMayUse() {
        assertEquals(true, BackupCrypto.ITERATIONS >= BackupCrypto.MIN_ITERATIONS);
        assertEquals(true, BackupCrypto.ITERATIONS <= BackupCrypto.MAX_ITERATIONS);
    }
}
