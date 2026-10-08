package de.mcmodersd.unipensum.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.data.db.DbHelper;
import de.mcmodersd.unipensum.data.db.Schema;
import de.mcmodersd.unipensum.domain.backup.BackupCleaner;
import de.mcmodersd.unipensum.domain.backup.BackupData;
import de.mcmodersd.unipensum.domain.backup.BackupInfo;
import de.mcmodersd.unipensum.domain.backup.BackupReport;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

@RunWith(AndroidJUnit4.class)
public class BackupFileTest {

    /** Few iterations: the real count would only make the tests slow. */
    private static final int ITERATIONS = BackupCrypto.MIN_ITERATIONS;
    private static final char[] PASSWORD = "correct horse".toCharArray();
    private static final BackupFile.Meta META =
            new BackupFile.Meta(Schema.VERSION, 1, "1.0", Instant.parse("2026-10-05T10:00:00Z"));

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2027, 1, 22);

    private DbHelper helper;
    private SQLiteDatabase db;

    @Before
    public void openDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        helper = new DbHelper(context, null);
        db = helper.getWritableDatabase();
    }

    @After
    public void closeDatabase() {
        helper.close();
    }

    /** Two lecturers, a semester with a custom name, a course with a Moodle link and two events, one session moved. */
    private BackupData sample() {
        var weber = TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "Weber", "anna@uni.example", "+49 30 123"));
        var koch = TimetableStore.saveLecturer(db, new Lecturer(0, "", "Prof. Koch", null, null));
        var semester = TimetableStore.saveSemester(db, new Semester(0, START, END, "Winter term"));

        var lecture = new SessionDetails(
                SessionType.LECTURE, 480, 675, Mode.IN_PERSON, true,
                "A1", "https://meet.example/x", weber, "bring laptop\n\nroom B2", 45
        );
        var exercise = new SessionDetails(
                SessionType.EXERCISE, 600, 700, Mode.ONLINE, false,
                null, "https://meet.example/y", koch, null, SessionDetails.NO_REMINDER
        );
        TimetableStore.createCourse(
                db,
                new Course(0, semester, "Math", CourseColor.TEAL, "https://moodle.example/c/1"),
                List.of(
                        new Series(0, 0, lecture, new Schedule(DayOfWeek.MONDAY, START, END, 1)),
                        new Series(0, 0, exercise, new Schedule(DayOfWeek.THURSDAY, START, END, 2))
                )
        );

        // One session of the lecture deviates: another day and room.
        var first = TimetableStore.loadTimetable(db).on(START.plusWeeks(2)).get(0).session();
        var moved = new SessionDetails(
                SessionType.LECTURE, 480, 675, Mode.IN_PERSON, true,
                "B7", "https://meet.example/x", weber, "bring laptop\n\nroom B2", 45
        );
        TimetableStore.editSession(db, first.id(), EditScope.THIS_ONLY, moved, START.plusWeeks(2).plusDays(1), null);
        return TimetableStore.exportData(db);
    }

    private static BackupFile.Opened open(byte[] file) throws BackupException {
        return BackupFile.open(file);
    }

    private static BackupException.Reason reasonOf(ThrowingRunnable action) {
        return assertThrows(BackupException.class, action).reason();
    }

    // --- round trips ---

    @Test
    public void withoutAPassword_everythingComesBack() throws Exception {
        var data = sample();

        var file = BackupFile.write(data, META, null);
        var opened = open(file);
        var result = opened.read(null);

        assertFalse(opened.isEncrypted());
        assertEquals(data, result.data());
        assertEquals(new BackupReport(2, 1, 1, 2, data.sessions().size(), 0, 0), result.report());
        assertTrue(result.report().isClean());
    }

    @Test
    public void theManifestSaysWhichVersionWroteTheFile() throws Exception {
        var info = open(BackupFile.write(sample(), META, null)).info();

        assertEquals(BackupFile.FORMAT, info.format());
        assertEquals(Schema.VERSION, info.schema());
        assertEquals(1, info.appVersionCode());
        assertEquals("1.0", info.appVersion());
        assertEquals(Instant.parse("2026-10-05T10:00:00Z"), info.exportedAt());
        assertEquals(BackupInfo.Compatibility.SAME, info.compatibility(BackupFile.FORMAT, Schema.VERSION, 1));
    }

    @Test
    public void theFileIsAZipArchiveWithAReadableManifest() throws Exception {
        var entries = unzip(BackupFile.write(sample(), META, null));

        assertEquals(2, entries.size());
        var manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertTrue(manifest, manifest.contains("\"app\":\"UniPensum\""));
        assertTrue(manifest, manifest.contains("\"schema\":" + Schema.VERSION));
        assertFalse(manifest, manifest.contains("encryption"));
        assertTrue(entries.containsKey("data.json"));
    }

    @Test
    public void withAPassword_theDataIsNotReadableWithoutIt() throws Exception {
        var data = sample();

        var file = BackupFile.write(data, META, PASSWORD.clone(), ITERATIONS);
        var entries = unzip(file);

        assertTrue(entries.containsKey("data.enc"));
        assertFalse(entries.containsKey("data.json"));
        var manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertTrue(manifest, manifest.contains("AES-256-GCM"));
        assertTrue(manifest, manifest.contains("\"iterations\":" + ITERATIONS));
        // The lecturer's name appears nowhere in the file, not even compressed away.
        assertEquals(-1, indexOf(entries.get("data.enc"), "Weber".getBytes(StandardCharsets.UTF_8)));

        var opened = open(file);
        assertTrue(opened.isEncrypted());
        assertEquals(data, opened.read(PASSWORD.clone()).data());
    }

    @Test
    public void aWrongOrMissingPassword_isRefused() throws Exception {
        var opened = open(BackupFile.write(sample(), META, PASSWORD.clone(), ITERATIONS));

        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(() -> opened.read("wrong horse".toCharArray())));
        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(() -> opened.read(null)));
        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(() -> opened.read(new char[0])));
        // A wrong try does not spoil the file: the right password still works.
        assertFalse(opened.read(PASSWORD.clone()).data().sessions().isEmpty());
    }

    @Test
    public void aChangedManifest_makesAProtectedBackupFail() throws Exception {
        var file = BackupFile.write(sample(), META, PASSWORD.clone(), ITERATIONS);
        var entries = unzip(file);
        var manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        entries.put(
                "manifest.json", manifest.replace("\"appVersion\":\"1.0\"", "\"appVersion\":\"6.6\"")
                        .getBytes(StandardCharsets.UTF_8)
        );

        var opened = open(zip(entries));

        assertEquals("6.6", opened.info().appVersion());
        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(() -> opened.read(PASSWORD.clone())));
    }

    @Test
    public void changedEncryptedData_makesAProtectedBackupFail() throws Exception {
        var entries = unzip(BackupFile.write(sample(), META, PASSWORD.clone(), ITERATIONS));
        var data = entries.get("data.enc");
        data[data.length / 2] ^= 0x01;

        var opened = open(zip(entries));

        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(() -> opened.read(PASSWORD.clone())));
    }

    // --- files that are not (good) backups ---

    @Test
    public void anotherKindOfFile_isNotABackup() {
        assertEquals(
                BackupException.Reason.NOT_A_BACKUP,
                reasonOf(() -> open("just some text".getBytes(StandardCharsets.UTF_8)))
        );
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf(() -> open(new byte[0])));
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf(() -> open(new byte[]{0x1f, (byte) 0x8b, 8, 0, 0})));
    }

    @Test
    public void aZipWithoutOurManifest_isNotABackup() throws Exception {
        var other = new LinkedHashMap<String, byte[]>();
        other.put("photo.jpg", new byte[]{1, 2, 3});
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf(() -> open(zip(other))));

        var foreign = new LinkedHashMap<String, byte[]>();
        foreign.put("manifest.json", "{\"app\":\"SomethingElse\",\"format\":1,\"schema\":1}".getBytes(StandardCharsets.UTF_8));
        foreign.put("data.json", "{}".getBytes(StandardCharsets.UTF_8));
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf(() -> open(zip(foreign))));
    }

    @Test
    public void aTruncatedFile_isDamaged() throws Exception {
        var file = BackupFile.write(sample(), META, null);
        // Cut a few bytes into the second entry, the data.
        var cut = secondEntry(file) + 39 + 10;

        var truncated = Arrays.copyOf(file, cut);

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(() -> open(truncated)));
    }

    @Test
    public void aManifestThatIsNoJson_isDamaged() throws Exception {
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("manifest.json", "this is not json".getBytes(StandardCharsets.UTF_8));
        entries.put("data.json", "{}".getBytes(StandardCharsets.UTF_8));

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(() -> open(zip(entries))));
    }

    @Test
    public void aBackupWithoutItsData_isDamaged() throws Exception {
        var entries = unzip(BackupFile.write(sample(), META, null));
        entries.remove("data.json");

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(() -> open(zip(entries))));
    }

    @Test
    public void dataThatIsNoJson_isDamaged() throws Exception {
        var entries = unzip(BackupFile.write(sample(), META, null));
        entries.put("data.json", "[1, 2, 3]".getBytes(StandardCharsets.UTF_8));

        var opened = open(zip(entries));

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(() -> opened.read(null)));
    }

    @Test
    public void aFileLargerThanAnyBackup_isRefusedBeforeItIsOpened() {
        assertEquals(
                BackupException.Reason.TOO_LARGE,
                reasonOf(() -> open(new byte[(int) BackupFile.MAX_FILE_BYTES + 1]))
        );
    }

    @Test
    public void aSmallFileThatUnpacksToTooMuch_isRefused() throws Exception {
        var bomb = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bomb)) {
            zip.putNextEntry(new ZipEntry("data.json"));
            var zeros = new byte[1024 * 1024];
            for (var i = 0; i < 70; i++) zip.write(zeros);
            zip.closeEntry();
        }
        var file = bomb.toByteArray();

        assertTrue("The test file should be small, was " + file.length, file.length < BackupFile.MAX_FILE_BYTES);
        assertEquals(BackupException.Reason.TOO_LARGE, reasonOf(() -> open(file)));
    }

    @Test
    public void aProtectionThisVersionDoesNotKnow_isReportedAsSuch() throws Exception {
        var manifest = "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2,\"encryption\":"
                + "{\"cipher\":\"ChaCha20\",\"kdf\":\"Argon2\",\"iterations\":3,\"salt\":\"AAAA\",\"iv\":\"AAAA\"}}";
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("manifest.json", manifest.getBytes(StandardCharsets.UTF_8));
        entries.put("data.enc", new byte[]{1, 2, 3});

        assertEquals(BackupException.Reason.UNSUPPORTED, reasonOf(() -> open(zip(entries))));
    }

    @Test
    public void implausibleKeyParameters_areRefused() throws Exception {
        for (var iterations : new int[]{1, 5_000, 50_000_000}) {
            var manifest = "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2,\"encryption\":"
                    + "{\"cipher\":\"AES-256-GCM\",\"kdf\":\"PBKDF2WithHmacSHA256\",\"iterations\":" + iterations
                    + ",\"salt\":\"AAAAAAAAAAAAAAAAAAAAAA==\",\"iv\":\"AAAAAAAAAAAAAAAA\"}}";
            var entries = new LinkedHashMap<String, byte[]>();
            entries.put("manifest.json", manifest.getBytes(StandardCharsets.UTF_8));
            entries.put("data.enc", new byte[]{1, 2, 3});

            assertEquals("iterations " + iterations, BackupException.Reason.DAMAGED, reasonOf(() -> open(zip(entries))));
        }
    }

    // --- other versions ---

    @Test
    public void aFixedSampleOfTheCurrentFormat_isStillUnderstood() throws Exception {
        // Do not "fix" this sample when the code changes: it stands for files that already exist.
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2,\"appVersion\":\"1.0\",\"appVersionCode\":1,"
                                        + "\"exportedAt\":\"2026-10-05T10:00:00Z\"}",
                                "{\"lecturers\":[{\"id\":1,\"firstName\":\"Anna\",\"lastName\":\"Weber\",\"email\":\"anna@uni.example\","
                                        + "\"phone\":\"+49 30 123\"},{\"id\":2,\"firstName\":\"\",\"lastName\":\"Prof. Koch\"}],"
                                        + "\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\",\"name\":\"Winter term\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\",\"color\":\"blue\","
                                        + "\"moodle\":\"https://moodle.example/c/1\"}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":1,\"first\":\"2026-10-05\",\"last\":\"2026-10-12\","
                                        + "\"interval\":1,\"type\":\"lecture\",\"startMin\":480,\"endMin\":675,\"mode\":\"in_person\","
                                        + "\"hybrid\":true,\"room\":\"A1\",\"link\":\"https://meet.example/x\",\"lecturer\":1,"
                                        + "\"note\":\"bring laptop\"}],"
                                        + "\"sessions\":[{\"id\":1,\"series\":1,\"day\":\"2026-10-05\",\"type\":\"lecture\","
                                        + "\"startMin\":480,\"endMin\":675,\"mode\":\"in_person\",\"hybrid\":true,\"room\":\"A1\","
                                        + "\"link\":\"https://meet.example/x\",\"lecturer\":1,\"note\":\"bring laptop\"},"
                                        + "{\"id\":2,\"series\":1,\"day\":\"2026-10-13\",\"type\":\"exercise\",\"startMin\":600,"
                                        + "\"endMin\":700,\"mode\":\"online\",\"link\":\"https://meet.example/y\",\"lecturer\":2}]}"
                        )
                )
        );

        var result = opened.read(null);

        assertEquals(new BackupReport(2, 1, 1, 1, 2, 0, 0), result.report());
        var data = result.data();
        assertEquals("anna@uni.example", data.lecturers().get(0).email());
        assertEquals("", data.lecturers().get(1).firstName());
        assertEquals("Winter term", data.semesters().get(0).customName());
        assertEquals(LocalDate.of(2027, 1, 22), data.semesters().get(0).end());
        assertEquals(CourseColor.BLUE, data.courses().get(0).color());
        assertEquals("https://moodle.example/c/1", data.courses().get(0).moodleLink());
        assertEquals(DayOfWeek.MONDAY, data.series().get(0).schedule().weekday());
        assertEquals(1, data.series().get(0).details().lecturerId());
        var moved = data.sessions().get(1);
        assertEquals(LocalDate.of(2026, 10, 13), moved.day());
        assertEquals(SessionType.EXERCISE, moved.details().type());
        assertEquals(Mode.ONLINE, moved.details().mode());
        assertNull(moved.details().room());
        assertEquals(2, moved.details().lecturerId());
    }

    @Test
    public void aFileFromBeforeTheReminders_getsTheDefaultOfEachEvent() throws Exception {
        // Do not "fix" this sample when the code changes: it stands for files that already exist.
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2,\"appVersion\":\"1.0\",\"appVersionCode\":1}",
                                "{\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\",\"color\":\"red\"}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":1,\"first\":\"2026-10-05\",\"last\":\"2026-10-12\","
                                        + "\"type\":\"lecture\",\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\"},"
                                        + "{\"id\":2,\"course\":1,\"weekday\":2,\"first\":\"2026-10-06\",\"last\":\"2026-10-13\","
                                        + "\"type\":\"lab\",\"startMin\":480,\"endMin\":600,\"mode\":\"online\"}],"
                                        + "\"sessions\":[{\"id\":1,\"series\":1,\"day\":\"2026-10-05\",\"type\":\"lecture\","
                                        + "\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\"},"
                                        + "{\"id\":2,\"series\":2,\"day\":\"2026-10-06\",\"type\":\"lab\",\"startMin\":480,"
                                        + "\"endMin\":600,\"mode\":\"online\"}]}"
                        )
                )
        );

        var result = opened.read(null);

        // Nothing was changed by the reader: a file that never had the field is not a damaged one.
        assertEquals(new BackupReport(0, 1, 1, 2, 2, 0, 0), result.report());
        assertEquals(30, result.data().series().get(0).details().reminderMin());
        assertEquals(5, result.data().series().get(1).details().reminderMin());
        assertEquals(30, result.data().sessions().get(0).details().reminderMin());
        assertEquals(5, result.data().sessions().get(1).details().reminderMin());
    }

    @Test
    public void remindersInAFile_areKeptBroughtIntoRangeOrReplacedByTheDefault() throws Exception {
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":3}",
                                "{\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\",\"color\":\"red\"}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":1,\"first\":\"2026-10-05\",\"last\":\"2026-10-05\","
                                        + "\"type\":\"lecture\",\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\",\"reminder\":0},"
                                        + "{\"id\":2,\"course\":1,\"weekday\":2,\"first\":\"2026-10-06\",\"last\":\"2026-10-06\","
                                        + "\"type\":\"lab\",\"startMin\":480,\"endMin\":600,\"mode\":\"online\",\"reminder\":-1},"
                                        + "{\"id\":3,\"course\":1,\"weekday\":3,\"first\":\"2026-10-07\",\"last\":\"2026-10-07\","
                                        + "\"type\":\"lab\",\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\",\"reminder\":9999},"
                                        + "{\"id\":4,\"course\":1,\"weekday\":4,\"first\":\"2026-10-08\",\"last\":\"2026-10-08\","
                                        + "\"type\":\"lab\",\"startMin\":480,\"endMin\":600,\"mode\":\"online\",\"reminder\":\"soon\"}]}"
                        )
                )
        );

        var result = opened.read(null);

        var series = result.data().series();
        assertEquals(0, series.get(0).details().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, series.get(1).details().reminderMin());
        assertEquals(SessionDetails.MAX_REMINDER_MIN, series.get(2).details().reminderMin());
        assertEquals(5, series.get(3).details().reminderMin());
        // Only the value that is no number counts as adjusted.
        assertEquals(new BackupReport(0, 1, 1, 4, 0, 0, 1), result.report());
    }

    @Test
    public void aNewerVersionsFile_isReadAsFarAsItIsUnderstood() throws Exception {
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":99,\"appVersion\":\"9.9\",\"appVersionCode\":999,"
                                        + "\"somethingNew\":{\"x\":1}}",
                                "{\"futureThings\":[1,2,3],"
                                        + "\"lecturers\":[{\"id\":1,\"firstName\":\"Anna\",\"lastName\":\"Weber\",\"office\":\"B12\"}],"
                                        + "\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\",\"color\":\"magenta\"}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":1,\"first\":\"2026-10-05\",\"last\":\"2026-10-12\","
                                        + "\"type\":\"workshop\",\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\"}],"
                                        + "\"sessions\":[{\"id\":1,\"series\":1,\"day\":\"2026-10-05\",\"type\":\"lecture\","
                                        + "\"startMin\":480,\"endMin\":600,\"mode\":\"hologram\"}]}"
                        )
                )
        );

        var result = opened.read(null);

        assertEquals(BackupInfo.Compatibility.NEWER, opened.info().compatibility(BackupFile.FORMAT, Schema.VERSION, 1));
        // Taken over, with a default where the value is unknown: the color, the event's type, the session's mode.
        assertEquals(new BackupReport(1, 1, 1, 1, 1, 0, 3), result.report());
        assertEquals(CourseColor.BLUE, result.data().courses().get(0).color());
        assertEquals(SessionType.LECTURE, result.data().series().get(0).details().type());
        assertEquals(Mode.IN_PERSON, result.data().sessions().get(0).details().mode());
    }

    @Test
    public void entriesThatCannotBeReadAreSkippedAndCounted_theRestIsTakenOver() throws Exception {
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2}",
                                "{\"lecturers\":[{\"id\":1,\"lastName\":\"Weber\"},{\"id\":2,\"firstName\":\"X\"},42],"
                                        + "\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"},"
                                        + "{\"id\":2,\"start\":\"2026-13-45\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\"},{\"id\":2,\"semester\":1}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":6,\"first\":\"2026-10-05\",\"last\":\"2026-10-12\","
                                        + "\"startMin\":480,\"endMin\":600}]}"
                        )
                )
        );

        var result = opened.read(null);

        // Skipped: a lecturer without a last name, a value that is no object, an impossible date, a course
        // without a name, a series on a Saturday. Adjusted: the course that names no color.
        assertEquals(new BackupReport(1, 1, 1, 0, 0, 5, 1), result.report());
        assertEquals("Weber", result.data().lecturers().get(0).lastName());
    }

    @Test
    public void optionalFieldsMayBeMissing() throws Exception {
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2}",
                                "{\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"Math\",\"color\":\"red\"}],"
                                        + "\"series\":[{\"id\":1,\"course\":1,\"weekday\":3,\"first\":\"2026-10-07\",\"last\":\"2026-10-07\","
                                        + "\"type\":\"lab\",\"startMin\":480,\"endMin\":600,\"mode\":\"in_person\"}]}"
                        )
                )
        );

        var result = opened.read(null);

        assertEquals(new BackupReport(0, 1, 1, 1, 0, 0, 0), result.report());
        var details = result.data().series().get(0).details();
        assertEquals(1, result.data().series().get(0).schedule().intervalWeeks());
        assertFalse(details.hybrid());
        assertNull(details.room());
        assertNull(details.link());
        assertNull(details.note());
        assertEquals(SessionDetails.NO_LECTURER, details.lecturerId());
        assertNull(result.data().semesters().get(0).customName());
        assertNull(result.data().courses().get(0).moodleLink());
    }

    @Test
    public void textInAFileIsCleanedLikeTypedText() throws Exception {
        var opened = open(
                zip(
                        sampleFile(
                                "{\"app\":\"UniPensum\",\"format\":1,\"schema\":2}",
                                "{\"lecturers\":[{\"id\":1,\"firstName\":\" Anna\\u200b \",\"lastName\":\"  Weber \\t Koch\","
                                        + "\"email\":\" a @uni.example \"}],"
                                        + "\"semesters\":[{\"id\":1,\"start\":\"2026-10-05\",\"end\":\"2027-01-22\"}],"
                                        + "\"courses\":[{\"id\":1,\"semester\":1,\"name\":\"  Math \\u202e II \",\"moodle\":\"javascript:alert(1)\"}]}"
                        )
                )
        );

        var result = opened.read(null);

        assertEquals("Anna", result.data().lecturers().get(0).firstName());
        assertEquals("Weber Koch", result.data().lecturers().get(0).lastName());
        assertEquals("a@uni.example", result.data().lecturers().get(0).email());
        assertEquals("Math II", result.data().courses().get(0).name());
        assertNull(result.data().courses().get(0).moodleLink());
    }

    // --- helpers ---

    private static Map<String, byte[]> sampleFile(String manifest, String data) {
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("manifest.json", manifest.getBytes(StandardCharsets.UTF_8));
        entries.put("data.json", data.getBytes(StandardCharsets.UTF_8));
        return entries;
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static Map<String, byte[]> unzip(byte[] file) throws IOException {
        var entries = new LinkedHashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                var content = new ByteArrayOutputStream();
                var buffer = new byte[4096];
                int read;
                while ((read = zip.read(buffer)) != -1) content.write(buffer, 0, read);
                entries.put(entry.getName(), content.toByteArray());
            }
        }
        return entries;
    }

    /** Where the second local file header of a zip begins. */
    private static int secondEntry(byte[] file) {
        byte[] signature = {0x50, 0x4b, 0x03, 0x04};
        var first = indexOf(file, signature, 0);
        return indexOf(file, signature, first + 1);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        return indexOf(haystack, needle, 0);
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (var i = from; i <= haystack.length - needle.length; i++) {
            for (var j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
