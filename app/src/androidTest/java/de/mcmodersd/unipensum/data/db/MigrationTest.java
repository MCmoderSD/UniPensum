package de.mcmodersd.unipensum.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import de.mcmodersd.unipensum.data.ReminderView;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/**
 * Builds a database exactly as version 1 of the app wrote it (free-text lecturers) and opens it with the
 * current {@link DbHelper}, which migrates it to the current version.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String NAME = "migration-test.db";
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 12);

    /** The version 1 layout, copied from the schema of that release. */
    private static final String[] VERSION_1 = {
            "CREATE TABLE semester (id INTEGER PRIMARY KEY AUTOINCREMENT, start_day INTEGER NOT NULL, "
                    + "end_day INTEGER NOT NULL, custom_name TEXT)",
            "CREATE TABLE course (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "semester_id INTEGER NOT NULL REFERENCES semester(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, color TEXT NOT NULL)",
            "CREATE TABLE series (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "course_id INTEGER NOT NULL REFERENCES course(id) ON DELETE CASCADE, "
                    + "type TEXT NOT NULL, weekday INTEGER NOT NULL, start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, mode TEXT NOT NULL, hybrid INTEGER NOT NULL, room TEXT, "
                    + "link TEXT, lecturer TEXT, note TEXT, first_day INTEGER NOT NULL, "
                    + "last_day INTEGER NOT NULL, interval_weeks INTEGER NOT NULL)",
            "CREATE TABLE session (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "series_id INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE, "
                    + "day INTEGER NOT NULL, type TEXT NOT NULL, start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, mode TEXT NOT NULL, hybrid INTEGER NOT NULL, room TEXT, "
                    + "link TEXT, lecturer TEXT, note TEXT)",
            "CREATE INDEX course_semester ON course(semester_id)",
            "CREATE INDEX series_course ON series(course_id)",
            "CREATE INDEX session_series ON session(series_id)",
            "CREATE INDEX session_day ON session(day)"
    };

    private Context context;

    @Before
    public void createVersion1Database() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(NAME);

        SQLiteDatabase old = context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        for (String statement : VERSION_1) old.execSQL(statement);
        old.execSQL("INSERT INTO semester VALUES (1, " + MONDAY.toEpochDay() + ", "
                + LocalDate.of(2027, 2, 12).toEpochDay() + ", NULL)");
        old.execSQL("INSERT INTO course (id, semester_id, name, color) VALUES (1, 1, 'Math', 'blue')");
        // Series 1: text with spaces around it. Series 2: the same text. Series 3: none. Series 4: only blanks.
        series(old, 1, DayOfWeek.MONDAY, "'  Prof. Weber '");
        series(old, 2, DayOfWeek.TUESDAY, "'Prof. Weber'");
        series(old, 3, DayOfWeek.WEDNESDAY, "NULL");
        series(old, 4, DayOfWeek.THURSDAY, "'   '");
        // Sessions 1 and 2 belong to series 1; the second one was given another lecturer by hand.
        session(old, 1, 1, MONDAY, "'  Prof. Weber '");
        session(old, 2, 1, NEXT_MONDAY, "'Dr. Lang'");
        session(old, 3, 2, MONDAY.plusDays(1), "'Prof. Weber'");
        session(old, 4, 3, MONDAY.plusDays(2), "NULL");
        session(old, 5, 4, MONDAY.plusDays(3), "'   '");
        old.setVersion(1);
        old.close();
    }

    @After
    public void deleteDatabase() {
        context.deleteDatabase(NAME);
    }

    private static void series(SQLiteDatabase db, int id, DayOfWeek weekday, String lecturer) {
        db.execSQL("INSERT INTO series (id, course_id, type, weekday, start_min, end_min, mode, hybrid, room, link, "
                + "lecturer, note, first_day, last_day, interval_weeks) VALUES (" + id + ", 1, 'lecture', "
                + weekday.getValue() + ", 480, 570, 'in_person', 0, 'A1', NULL, " + lecturer + ", NULL, "
                + MONDAY.toEpochDay() + ", " + NEXT_MONDAY.plusDays(4).toEpochDay() + ", 1)");
    }

    private static void session(SQLiteDatabase db, int id, int seriesId, LocalDate day, String lecturer) {
        db.execSQL("INSERT INTO session (id, series_id, day, type, start_min, end_min, mode, hybrid, room, link, "
                + "lecturer, note) VALUES (" + id + ", " + seriesId + ", " + day.toEpochDay() + ", 'lecture', 480, 570, "
                + "'in_person', 0, 'A1', NULL, " + lecturer + ", NULL)");
    }

    private SQLiteDatabase migrate() {
        return new DbHelper(context, NAME).getWritableDatabase();
    }

    @Test
    public void everyDistinctTextBecomesOneLecturerWithThatLastName() {
        SQLiteDatabase db = migrate();

        List<Lecturer> lecturers = TimetableStore.listLecturers(db);

        // "Dr. Lang" and "Prof. Weber" once each, although Weber appeared with and without spaces.
        assertEquals(2, lecturers.size());
        assertEquals("Dr. Lang", lecturers.get(0).lastName());
        assertEquals("Prof. Weber", lecturers.get(1).lastName());
        for (Lecturer lecturer : lecturers) {
            assertEquals("", lecturer.firstName());
            assertNull(lecturer.email());
            assertNull(lecturer.phone());
        }
        db.close();
    }

    @Test
    public void seriesAndSessionsPointAtTheirLecturerAndKeepTheirDeviations() {
        SQLiteDatabase db = migrate();
        long lang = TimetableStore.listLecturers(db).get(0).id();
        long weber = TimetableStore.listLecturers(db).get(1).id();

        List<Series> series = TimetableStore.listCourses(db, 1).get(0).series();
        assertEquals(weber, series.get(0).details().lecturerId());                         // Monday
        assertEquals(weber, series.get(1).details().lecturerId());                         // Tuesday
        assertEquals(SessionDetails.NO_LECTURER, series.get(2).details().lecturerId());    // Wednesday
        assertEquals(SessionDetails.NO_LECTURER, series.get(3).details().lecturerId());    // Thursday

        Timetable timetable = TimetableStore.loadTimetable(db);
        SessionView first = timetable.on(MONDAY).get(0);
        SessionView deviating = timetable.on(NEXT_MONDAY).get(0);
        assertEquals(weber, first.session().details().lecturerId());
        assertEquals("Prof. Weber", first.lecturer().lastName());
        assertEquals(lang, deviating.session().details().lecturerId());
        assertEquals("Dr. Lang", deviating.lecturer().lastName());
        assertNull(timetable.on(MONDAY.plusDays(2)).get(0).lecturer());
        assertNull(timetable.on(MONDAY.plusDays(3)).get(0).lecturer());
        db.close();
    }

    @Test
    public void nothingElseIsLostAndTheOldTextIsGone() {
        SQLiteDatabase db = migrate();

        assertEquals(2, DatabaseUtils.queryNumEntries(db, "series", "weekday <= 2"));
        assertEquals(4, DatabaseUtils.queryNumEntries(db, "series"));
        assertEquals(5, DatabaseUtils.queryNumEntries(db, "session"));
        assertEquals(1, DatabaseUtils.queryNumEntries(db, "course"));
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "series", "lecturer IS NOT NULL"));
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "session", "lecturer IS NOT NULL"));
        assertEquals(Schema.VERSION, db.getVersion());
        assertNull(TimetableStore.loadCourse(db, 1).course().moodleLink());
        db.close();
    }

    @Test
    public void theMigratedDatabaseKeepsWorking() {
        SQLiteDatabase db = migrate();
        long weber = TimetableStore.listLecturers(db).get(1).id();

        // New data on top of the migrated layout, which still has the unused text columns.
        long created = TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "Neu", "neu@uni.example", null));
        SessionDetails details = new SessionDetails(SessionType.EXERCISE, 600, 700, Mode.IN_PERSON, false,
                "B1", null, created, null, SessionDetails.NO_REMINDER);
        TimetableStore.addSeries(db, 1, new Series(0, 0, details,
                new Schedule(DayOfWeek.FRIDAY, MONDAY, NEXT_MONDAY.plusDays(4), 1)));
        assertEquals(created, TimetableStore.loadTimetable(db).on(MONDAY.plusDays(4)).get(0).session().details().lecturerId());

        // Deleting a lecturer still clears the references (foreign keys survived the ALTER TABLE).
        TimetableStore.deleteLecturer(db, weber);
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "session", "lecturer_id = " + weber));
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "series", "lecturer_id = " + weber));
        assertEquals(5 + 2, DatabaseUtils.queryNumEntries(db, "session"));

        // The columns that were added are indexed.
        try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE '%lecturer'", null)) {
            assertEquals(2, cursor.getCount());
        }
        assertNotEquals(0, created);
        assertTrue(DatabaseUtils.queryNumEntries(db, "lecturer") >= 2);
        db.close();
    }

    @Test
    public void fromVersion1_everyEventAndSessionGetsTheDefaultReminder() {
        SQLiteDatabase db = migrate();

        // All of the version 1 rows are in person.
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "series", "reminder_min IS NULL OR reminder_min <> 30"));
        assertEquals(0, DatabaseUtils.queryNumEntries(db, "session", "reminder_min IS NULL OR reminder_min <> 30"));
        List<ReminderView> reminders = TimetableStore.loadReminders(db, MONDAY);
        assertEquals(5, reminders.size());
        for (ReminderView view : reminders) assertEquals(30, view.session().details().reminderMin());
        db.close();
    }

    /** The version 2 layout, copied from the schema of that release. */
    private static final String[] VERSION_2 = {
            "CREATE TABLE semester (id INTEGER PRIMARY KEY AUTOINCREMENT, start_day INTEGER NOT NULL, "
                    + "end_day INTEGER NOT NULL, custom_name TEXT)",
            "CREATE TABLE lecturer (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT NOT NULL, "
                    + "last_name TEXT NOT NULL, email TEXT, phone TEXT)",
            "CREATE TABLE course (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "semester_id INTEGER NOT NULL REFERENCES semester(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, color TEXT NOT NULL, moodle_link TEXT)",
            "CREATE TABLE series (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "course_id INTEGER NOT NULL REFERENCES course(id) ON DELETE CASCADE, "
                    + "type TEXT NOT NULL, weekday INTEGER NOT NULL, start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, mode TEXT NOT NULL, hybrid INTEGER NOT NULL, room TEXT, "
                    + "link TEXT, lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL, note TEXT, "
                    + "first_day INTEGER NOT NULL, last_day INTEGER NOT NULL, interval_weeks INTEGER NOT NULL)",
            "CREATE TABLE session (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "series_id INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE, "
                    + "day INTEGER NOT NULL, type TEXT NOT NULL, start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, mode TEXT NOT NULL, hybrid INTEGER NOT NULL, room TEXT, "
                    + "link TEXT, lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL, note TEXT)"
    };

    @Test
    public void fromVersion2_onlineEventsGetFiveMinutesAndTheRestThirty() {
        context.deleteDatabase(NAME);
        SQLiteDatabase old = context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        for (String statement : VERSION_2) old.execSQL(statement);
        old.execSQL("INSERT INTO semester VALUES (1, " + MONDAY.toEpochDay() + ", "
                + LocalDate.of(2027, 2, 12).toEpochDay() + ", NULL)");
        old.execSQL("INSERT INTO course (id, semester_id, name, color) VALUES (1, 1, 'Math', 'blue')");
        // Series 1 is in person, series 2 online, series 3 hybrid.
        String[] modes = {"'in_person', 0", "'online', 0", "'in_person', 1"};
        for (int i = 0; i < 3; i++) {
            old.execSQL("INSERT INTO series (id, course_id, type, weekday, start_min, end_min, mode, hybrid, "
                    + "first_day, last_day, interval_weeks) VALUES (" + (i + 1) + ", 1, 'lecture', " + (i + 1)
                    + ", 480, 570, " + modes[i] + ", " + MONDAY.toEpochDay() + ", " + MONDAY.toEpochDay() + ", 1)");
            old.execSQL("INSERT INTO session (id, series_id, day, type, start_min, end_min, mode, hybrid) VALUES ("
                    + (i + 1) + ", " + (i + 1) + ", " + MONDAY.plusDays(i).toEpochDay() + ", 'lecture', 480, 570, "
                    + modes[i] + ")");
        }
        old.setVersion(2);
        old.close();

        SQLiteDatabase db = migrate();

        List<ReminderView> reminders = TimetableStore.loadReminders(db, MONDAY);
        assertEquals(3, reminders.size());
        assertEquals(30, reminders.get(0).session().details().reminderMin());
        assertEquals(5, reminders.get(1).session().details().reminderMin());
        assertEquals(30, reminders.get(2).session().details().reminderMin());
        assertEquals(5, TimetableStore.listCourses(db, 1).get(0).series().get(1).details().reminderMin());
        assertEquals(Schema.VERSION, db.getVersion());
        db.close();
    }

    @Test
    public void aFreshDatabaseHasTheSameShape() {
        context.deleteDatabase(NAME);

        SQLiteDatabase db = new DbHelper(context, NAME).getWritableDatabase();
        long id = TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "Weber", null, null));

        assertEquals(Schema.VERSION, db.getVersion());
        assertEquals(1, TimetableStore.listLecturers(db).size());
        assertEquals("Weber", TimetableStore.listLecturers(db).get(0).lastName());
        assertNotEquals(0, id);
        try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE '%lecturer'", null)) {
            assertEquals(2, cursor.getCount());
        }
        db.close();
    }
}
