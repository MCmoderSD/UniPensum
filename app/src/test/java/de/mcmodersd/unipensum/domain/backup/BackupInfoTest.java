package de.mcmodersd.unipensum.domain.backup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;

import de.mcmodersd.unipensum.domain.backup.BackupInfo.Compatibility;

public class BackupInfoTest {

    private static BackupInfo info(int format, int schema, int build) {
        return new BackupInfo(format, schema, build, "x", Instant.parse("2026-10-05T10:00:00Z"));
    }

    @Test
    public void sameVersions_areSame() {
        assertEquals(Compatibility.SAME, info(1, 2, 5).compatibility(1, 2, 5));
    }

    @Test
    public void aHigherBuild_isNewer_aLowerOne_isOlder() {
        assertEquals(Compatibility.NEWER, info(1, 2, 6).compatibility(1, 2, 5));
        assertEquals(Compatibility.OLDER, info(1, 2, 4).compatibility(1, 2, 5));
    }

    @Test
    public void theSchemaDecidesBeforeTheBuild() {
        assertEquals(Compatibility.NEWER, info(1, 3, 1).compatibility(1, 2, 99));
        assertEquals(Compatibility.OLDER, info(1, 1, 99).compatibility(1, 2, 1));
    }

    @Test
    public void theFormatDecidesBeforeEverythingElse() {
        assertEquals(Compatibility.NEWER, info(2, 1, 1).compatibility(1, 5, 99));
        assertEquals(Compatibility.OLDER, info(1, 9, 99).compatibility(2, 1, 1));
    }
}