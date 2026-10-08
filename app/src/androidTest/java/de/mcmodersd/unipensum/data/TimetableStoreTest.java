package de.mcmodersd.unipensum.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.data.db.DbHelper;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

@RunWith(AndroidJUnit4.class)
public class TimetableStoreTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);      // Monday
    private static final LocalDate END = LocalDate.of(2027, 2, 12);        // Friday

    private Context context;
    private DbHelper helper;
    private SQLiteDatabase db;
    /** The lecturer of {@link #lecture()}. */
    private long lecturerId;

    @Before
    public void openDatabase() {
        context = ApplicationProvider.getApplicationContext();
        helper = new DbHelper(context, null);
        db = helper.getWritableDatabase();
        lecturerId = TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "Example", "anna@uni.example", "+49 30 1"));
    }

    @After
    public void closeDatabase() {
        helper.close();
    }

    private static LocalDate date(int month, int day) {
        return LocalDate.of(month >= 9 ? 2026 : 2027, month, day);
    }

    private SessionDetails lecture() {
        return new SessionDetails(
                SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "A1", null, lecturerId, null, SessionDetails.NO_REMINDER
        );
    }

    private long semester(LocalDate start, LocalDate end) {
        return TimetableStore.saveSemester(db, new Semester(0, start, end, null));
    }

    private static Series series(SessionDetails details, DayOfWeek weekday, LocalDate first, LocalDate last, int interval) {
        return new Series(0, 0, details, new Schedule(weekday, first, last, interval));
    }

    private long course(long semesterId, Series... series) {
        return TimetableStore.createCourse(db, new Course(0, semesterId, "Math", CourseColor.BLUE, null), List.of(series));
    }

    private long rows(String table) {
        return DatabaseUtils.queryNumEntries(db, table);
    }

    private long sessionIdOn(LocalDate day) {
        return TimetableStore.loadTimetable(db).on(day).get(0).session().id();
    }

    @Test
    public void createCourse_generatesSessionsForEverySeries() {
        var semesterId = semester(START, END);
        course(
                semesterId,
                series(lecture(), DayOfWeek.MONDAY, START, END, 1),
                series(lecture(), DayOfWeek.THURSDAY, START, END, 2)
        );

        // 19 Mondays plus 10 Thursdays in every second week.
        assertEquals(29, rows("session"));
        var timetable = TimetableStore.loadTimetable(db);
        assertEquals(1, timetable.on(date(10, 5)).size());
        assertEquals("Math", timetable.on(date(10, 5)).get(0).courseName());
        assertEquals(CourseColor.BLUE, timetable.on(date(10, 5)).get(0).color());
        assertEquals(1, timetable.on(date(10, 8)).size());
        assertTrue(timetable.on(date(10, 15)).isEmpty());
        assertEquals(1, timetable.on(date(10, 22)).size());
    }

    @Test
    public void series_roundTripsEveryField() {
        var semesterId = semester(START, END);
        var hybrid = new SessionDetails(
                SessionType.TUTORIAL, 13 * 60 + 15, 14 * 60 + 45,
                Mode.IN_PERSON, true, "B 2.04", "https://meet.example/room", lecturerId, "Bring laptop", SessionDetails.NO_REMINDER
        );
        course(semesterId, series(hybrid, DayOfWeek.WEDNESDAY, date(10, 7), date(12, 16), 3));

        var stored = TimetableStore.listCourses(db, semesterId).get(0).series().get(0);

        assertEquals(hybrid, stored.details());
        assertEquals(new Schedule(DayOfWeek.WEDNESDAY, date(10, 7), date(12, 16), 3), stored.schedule());
    }

    @Test
    public void reminder_roundTripsInTheSeriesAndTheirSessions_andNoneStaysNone() {
        var semesterId = semester(START, END);
        course(
                semesterId,
                series(lecture().withReminder(45), DayOfWeek.MONDAY, START, date(10, 12), 1),
                series(lecture().withReminder(0), DayOfWeek.TUESDAY, START, date(10, 12), 1),
                series(lecture(), DayOfWeek.WEDNESDAY, START, date(10, 12), 1)
        );

        var stored = TimetableStore.listCourses(db, semesterId).get(0).series();
        var timetable = TimetableStore.loadTimetable(db);

        assertEquals(45, stored.get(0).details().reminderMin());
        assertEquals(0, stored.get(1).details().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, stored.get(2).details().reminderMin());
        assertEquals(45, timetable.on(date(10, 5)).get(0).session().details().reminderMin());
        assertEquals(0, timetable.on(date(10, 6)).get(0).session().details().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, timetable.on(date(10, 7)).get(0).session().details().reminderMin());
    }

    @Test
    public void reminder_isBroughtIntoItsRangeWhenSaved() {
        var semesterId = semester(START, END);
        course(
                semesterId, series(lecture().withReminder(5_000), DayOfWeek.MONDAY, START, START, 1),
                series(lecture().withReminder(-20), DayOfWeek.TUESDAY, START, date(10, 6), 1)
        );

        var stored = TimetableStore.listCourses(db, semesterId).get(0).series();

        assertEquals(SessionDetails.MAX_REMINDER_MIN, stored.get(0).details().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, stored.get(1).details().reminderMin());
    }

    @Test
    public void loadReminders_holdsTheSessionsWithAReminderFromTheDayOn() {
        var semesterId = semester(START, END);
        course(
                semesterId,
                series(lecture().withReminder(30), DayOfWeek.MONDAY, START, date(10, 19), 1),
                series(lecture(), DayOfWeek.THURSDAY, START, date(10, 19), 1)
        );
        TimetableStore.createCourse(
                db, new Course(0, semesterId, "Physics", CourseColor.RED, "https://moodle.example/p"),
                List.of(series(lecture().withReminder(5), DayOfWeek.TUESDAY, date(10, 13), date(10, 13), 1))
        );

        var reminders = TimetableStore.loadReminders(db, date(10, 12));

        // Monday 12th and 19th of "Math", Tuesday 13th of "Physics"; no Thursday, no Monday 5th.
        assertEquals(3, reminders.size());
        assertEquals(date(10, 12), reminders.get(0).session().day());
        assertEquals("Math", reminders.get(0).courseName());
        assertNull(reminders.get(0).moodleLink());
        assertEquals(date(10, 13), reminders.get(1).session().day());
        assertEquals("Physics", reminders.get(1).courseName());
        assertEquals("https://moodle.example/p", reminders.get(1).moodleLink());
        assertEquals(5, reminders.get(1).session().details().reminderMin());
        assertEquals(date(10, 19), reminders.get(2).session().day());
    }

    @Test
    public void editSession_changesTheReminderWithinItsScope() {
        var semesterId = semester(START, END);
        course(semesterId, series(lecture().withReminder(30), DayOfWeek.MONDAY, START, date(10, 26), 1));
        var schedule = new Schedule(DayOfWeek.MONDAY, START, date(10, 26), 1);

        TimetableStore.editSession(
                db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY,
                lecture().withReminder(10), date(10, 12), null
        );
        assertEquals(10, reminderOn(date(10, 12)));
        assertEquals(30, reminderOn(date(10, 5)));
        assertEquals(30, reminderOn(date(10, 19)));

        TimetableStore.editSession(
                db, sessionIdOn(date(10, 19)), EditScope.THIS_AND_FOLLOWING,
                lecture().withReminder(SessionDetails.NO_REMINDER), date(10, 19), schedule
        );
        assertEquals(30, reminderOn(date(10, 5)));
        assertEquals(10, reminderOn(date(10, 12)));
        assertEquals(SessionDetails.NO_REMINDER, reminderOn(date(10, 19)));
        assertEquals(SessionDetails.NO_REMINDER, reminderOn(date(10, 26)));
    }

    private int reminderOn(LocalDate day) {
        return TimetableStore.loadTimetable(db).on(day).get(0).session().details().reminderMin();
    }

    @Test
    public void createCourse_normalizesDetailsForTheChosenMode() {
        var semesterId = semester(START, END);
        var online = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.ONLINE, true,
                "Leftover room", "https://meet.example/x", SessionDetails.NO_LECTURER, "", SessionDetails.NO_REMINDER
        );
        course(semesterId, series(online, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1));

        var stored = TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details();

        assertNull(stored.room());
        assertEquals(false, stored.hybrid());
        assertEquals("https://meet.example/x", stored.link());
        assertEquals(SessionDetails.NO_LECTURER, stored.lecturerId());
        assertNull(stored.note());
    }

    @Test
    public void createCourse_dropsALecturerThatNoLongerExists() {
        var semesterId = semester(START, END);
        var gone = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.IN_PERSON, false,
                "A1", null, 4711, null, SessionDetails.NO_REMINDER
        );

        course(semesterId, series(gone, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1));

        assertEquals(
                SessionDetails.NO_LECTURER,
                TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId()
        );
        assertEquals(1, rows("session"));
    }

    // --- lecturers ---

    @Test
    public void lecturers_areSavedUpdatedAndListedByLastName() {
        var weber = TimetableStore.saveLecturer(db, new Lecturer(0, " Anna ", "Weber", " ", "  +49 1 "));
        var adler = TimetableStore.saveLecturer(db, new Lecturer(0, "", "adler", "adler@uni.example", null));

        TimetableStore.saveLecturer(db, new Lecturer(weber, "Anna", "Weber-Koch", "a@uni.example", null));

        var all = TimetableStore.listLecturers(db);
        // The one from setUp is "Example"; the list is ordered by last name regardless of case.
        assertEquals(
                List.of("adler", "Example", "Weber-Koch"),
                all.stream().map(Lecturer::lastName).collect(Collectors.toList())
        );
        var updated = all.get(2);
        assertEquals(weber, updated.id());
        assertEquals("a@uni.example", updated.email());
        assertNull(updated.phone());
        assertEquals(adler, all.get(0).id());
        assertEquals("", all.get(0).firstName());
    }

    @Test
    public void saveLecturer_trimsAndTurnsBlankContactsIntoNull() {
        var id = TimetableStore.saveLecturer(db, new Lecturer(0, " Anna ", " Weber ", " ", " +49 1 "));

        var stored = TimetableStore.listLecturers(db).stream().filter(l -> l.id() == id).findFirst().orElseThrow();
        assertEquals("Anna", stored.firstName());
        assertEquals("Weber", stored.lastName());
        assertNull(stored.email());
        assertEquals("+49 1", stored.phone());
    }

    @Test
    public void saveLecturer_requiresALastNameAndAKnownId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "  ", null, null))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TimetableStore.saveLecturer(db, new Lecturer(4711, "Anna", "Weber", null, null))
        );
    }

    @Test
    public void timetableAndSessionContext_carryTheLecturer() {
        var semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, date(10, 12), 1));

        var view = TimetableStore.loadTimetable(db).on(date(10, 5)).get(0);
        var context = TimetableStore.loadSessionContext(db, view.session().id());

        assertEquals("Example", view.lecturer().lastName());
        assertEquals("anna@uni.example", context.lecturer().email());
    }

    @Test
    public void sessionWithoutALecturer_hasNone() {
        var semesterId = semester(START, END);
        var none = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.IN_PERSON, false,
                "A1", null, SessionDetails.NO_LECTURER, null, SessionDetails.NO_REMINDER
        );
        course(semesterId, series(none, DayOfWeek.MONDAY, START, START, 1));

        var view = TimetableStore.loadTimetable(db).on(START).get(0);

        assertNull(view.lecturer());
        assertNull(TimetableStore.loadSessionContext(db, view.session().id()).lecturer());
    }

    @Test
    public void deleteLecturer_leavesTheirSessionsWithoutOne() {
        var semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, date(10, 12), 1));
        assertEquals(2, rows("session"));

        TimetableStore.deleteLecturer(db, lecturerId);

        assertEquals(2, rows("session"));
        assertEquals(1, rows("series"));
        assertEquals(
                SessionDetails.NO_LECTURER,
                TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId()
        );
        assertNull(TimetableStore.loadTimetable(db).on(START).get(0).lecturer());
        assertEquals(
                SessionDetails.NO_LECTURER,
                TimetableStore.loadTimetable(db).on(START).get(0).session().details().lecturerId()
        );
    }

    @Test
    public void editSession_changingTheLecturer_reachesTheWholeSeriesOrOneSession() {
        var semesterId = semester(START, END);
        var schedule = new Schedule(DayOfWeek.MONDAY, START, date(10, 26), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));
        var other = TimetableStore.saveLecturer(db, new Lecturer(0, "Max", "Other", null, null));
        var withOther = new SessionDetails(
                SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "A1", null, other, null, SessionDetails.NO_REMINDER
        );

        TimetableStore.editSession(db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY, withOther, date(10, 12), null);
        var timetable = TimetableStore.loadTimetable(db);
        assertEquals("Other", timetable.on(date(10, 12)).get(0).lecturer().lastName());
        assertEquals("Example", timetable.on(date(10, 5)).get(0).lecturer().lastName());

        TimetableStore.editSession(db, sessionIdOn(date(10, 5)), EditScope.ALL, withOther, null, schedule);
        timetable = TimetableStore.loadTimetable(db);
        for (var day : new int[]{5, 12, 19, 26}) {
            assertEquals("Other", timetable.on(date(10, day)).get(0).lecturer().lastName());
        }
        assertEquals(other, TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId());
    }

    // --- cleaning of typed text ---

    @Test
    public void createCourse_cleansTheNameTheLinksAndTheNotes() {
        var semesterId = semester(START, END);
        var dirty = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, " meet.example/x ", lecturerId, "  a \r\n\r\n\r\n b​ ", SessionDetails.NO_REMINDER
        );

        var courseId = TimetableStore.createCourse(
                db,
                new Course(0, semesterId, "  Math​ \t II ", CourseColor.BLUE, " moodle.example/c/1 "),
                List.of(series(dirty, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1))
        );

        var stored = TimetableStore.loadCourse(db, courseId);
        assertEquals("Math II", stored.course().name());
        assertEquals("https://moodle.example/c/1", stored.course().moodleLink());
        assertEquals("https://meet.example/x", stored.series().get(0).details().link());
        assertEquals("a\n\nb", stored.series().get(0).details().note());
    }

    @Test
    public void createCourse_refusesALinkThatIsNoWebLinkAndStoresNothing() {
        var semesterId = semester(START, END);

        assertThrows(IllegalArgumentException.class, () -> TimetableStore.createCourse(
                db,
                new Course(0, semesterId, "Math", CourseColor.BLUE, "javascript:alert(1)"),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1))
        ));
        var badMeeting = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, "ftp://files.example/x", lecturerId, null, SessionDetails.NO_REMINDER
        );
        assertThrows(IllegalArgumentException.class, () -> course(
                semesterId,
                series(badMeeting, DayOfWeek.MONDAY, START, START, 1)
        ));

        assertEquals(0, rows("course"));
        assertEquals(0, rows("series"));
    }

    @Test
    public void createCourse_refusesANameOfOnlyInvisibleCharacters() {
        var semesterId = semester(START, END);
        assertThrows(IllegalArgumentException.class, () -> TimetableStore.createCourse(
                db,
                new Course(0, semesterId, " ​‮\t ", CourseColor.BLUE, null),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1))
        ));
        assertEquals(0, rows("course"));
    }

    @Test
    public void saveSemester_cleansTheCustomNameAndTurnsBlankIntoNone() {
        var id = TimetableStore.saveSemester(db, new Semester(0, START, END, "  Winter​ \n term "));
        assertEquals("Winter term", TimetableStore.listSemesters(db).get(0).customName());

        TimetableStore.saveSemester(db, new Semester(id, START, END, " ​ "));
        assertNull(TimetableStore.listSemesters(db).get(0).customName());
    }

    @Test
    public void saveCourse_doesNotRejectAnUntouchedEventBecauseOfAnOldLink() {
        var semesterId = semester(START, END);
        var online = new SessionDetails(
                SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, "https://meet.example/x", lecturerId, null, SessionDetails.NO_REMINDER
        );
        var courseId = course(semesterId, series(online, DayOfWeek.MONDAY, START, date(10, 12), 1));
        // A link an earlier version accepted and today's rules would refuse.
        db.execSQL("UPDATE series SET link = 'ftp://old.example/x'");
        var stored = TimetableStore.loadCourse(db, courseId);

        TimetableStore.saveCourse(
                db, new Course(courseId, semesterId, "Renamed", CourseColor.TEAL, null),
                stored.series()
        );

        var reloaded = TimetableStore.loadCourse(db, courseId);
        assertEquals("Renamed", reloaded.course().name());
        assertEquals("ftp://old.example/x", reloaded.series().get(0).details().link());
    }

    // --- moodle link ---

    @Test
    public void moodleLink_isStoredTrimmedAndKeptWhenTheCourseIsSaved() {
        var semesterId = semester(START, END);
        var courseId = TimetableStore.createCourse(
                db,
                new Course(0, semesterId, "Math", CourseColor.BLUE, "  https://moodle.example/c/1 "),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1))
        );
        assertEquals("https://moodle.example/c/1", TimetableStore.loadCourse(db, courseId).course().moodleLink());

        TimetableStore.saveCourse(
                db, new Course(courseId, semesterId, "Math", CourseColor.BLUE, "https://moodle.example/c/2"),
                TimetableStore.loadCourse(db, courseId).series()
        );
        assertEquals("https://moodle.example/c/2", TimetableStore.loadCourse(db, courseId).course().moodleLink());

        TimetableStore.saveCourse(
                db, new Course(courseId, semesterId, "Math", CourseColor.BLUE, "   "),
                TimetableStore.loadCourse(db, courseId).series()
        );
        assertNull(TimetableStore.loadCourse(db, courseId).course().moodleLink());
    }

    @Test
    public void createCourse_rejectsScheduleOutsideTheSemester() {
        var semesterId = semester(START, END);
        assertThrows(IllegalArgumentException.class, () -> course(
                semesterId,
                series(lecture(), DayOfWeek.MONDAY, date(9, 28), date(10, 26), 1)
        ));
        // The whole transaction rolled back, not even the course remains.
        assertEquals(0, rows("course"));
    }

    @Test
    public void saveSemester_rejectsOverlapButAllowsNeighbours() {
        semester(START, END);

        assertThrows(IllegalArgumentException.class, () -> semester(date(2, 12), date(7, 31)));
        semester(date(2, 13), date(7, 31));

        assertEquals(2, TimetableStore.listSemesters(db).size());
    }

    @Test
    public void saveSemester_shorteningClipsSeries_extendingProlongsThem() {
        var semesterId = semester(START, date(11, 13));
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, date(11, 13), 1));
        assertEquals(6, rows("session"));

        TimetableStore.saveSemester(db, new Semester(semesterId, START, date(10, 30), null));
        assertEquals(4, rows("session"));
        assertEquals(date(10, 30), TimetableStore.listCourses(db, semesterId).get(0).series().get(0).schedule().last());

        TimetableStore.saveSemester(db, new Semester(semesterId, START, date(11, 27), null));
        assertEquals(8, rows("session"));
        assertEquals(date(11, 27), TimetableStore.listCourses(db, semesterId).get(0).series().get(0).schedule().last());
    }

    @Test
    public void deleteSemester_cascadesToEverythingBelow() {
        var semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, END, 1));

        TimetableStore.deleteSemester(db, semesterId);

        assertEquals(0, rows("course"));
        assertEquals(0, rows("series"));
        assertEquals(0, rows("session"));
    }

    @Test
    public void editSession_thisAndFollowing_splitsTheSeriesInTheDatabase() {
        var semesterId = semester(START, END);
        var schedule = new Schedule(DayOfWeek.MONDAY, START, date(11, 2), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));
        var third = sessionIdOn(date(10, 19));
        var moved = new SessionDetails(
                SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "B2", null, lecturerId, null, SessionDetails.NO_REMINDER
        );

        TimetableStore.editSession(db, third, EditScope.THIS_AND_FOLLOWING, moved, date(10, 19), schedule);

        var series = TimetableStore.listCourses(db, semesterId).get(0).series();
        assertEquals(2, series.size());
        assertEquals(5, rows("session"));
        var timetable = TimetableStore.loadTimetable(db);
        assertEquals("A1", timetable.on(date(10, 12)).get(0).session().details().room());
        assertEquals("B2", timetable.on(date(10, 19)).get(0).session().details().room());
        assertEquals("B2", timetable.on(date(11, 2)).get(0).session().details().room());
        // Both halves reference their own series.
        var firstHalf = timetable.on(date(10, 12)).get(0).session().seriesId();
        var secondHalf = timetable.on(date(10, 19)).get(0).session().seriesId();
        assertTrue(firstHalf != secondHalf);
        assertEquals(secondHalf, timetable.on(date(11, 2)).get(0).session().seriesId());
    }

    @Test
    public void editSession_thisOnly_movesJustOneSession() {
        var semesterId = semester(START, END);
        var schedule = new Schedule(DayOfWeek.MONDAY, START, date(10, 26), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));

        TimetableStore.editSession(db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY, lecture(), date(10, 14), null);

        var timetable = TimetableStore.loadTimetable(db);
        assertTrue(timetable.on(date(10, 12)).isEmpty());
        assertEquals(1, timetable.on(date(10, 14)).size());
        assertEquals(4, rows("session"));
        assertEquals(1, rows("series"));
    }

    @Test
    public void deleteSession_scopes() {
        var semesterId = semester(START, END);
        var schedule = new Schedule(DayOfWeek.MONDAY, START, date(11, 2), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));

        TimetableStore.deleteSession(db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY);
        assertEquals(4, rows("session"));

        TimetableStore.deleteSession(db, sessionIdOn(date(10, 26)), EditScope.THIS_AND_FOLLOWING);
        assertEquals(2, rows("session"));
        assertEquals(date(10, 25), TimetableStore.listCourses(db, semesterId).get(0).series().get(0).schedule().last());

        TimetableStore.deleteSession(db, sessionIdOn(date(10, 5)), EditScope.ALL);
        assertEquals(0, rows("session"));
        assertEquals(0, rows("series"));
        assertEquals(1, rows("course"));
    }

    @Test
    public void saveCourse_appliesNameAddedChangedAndRemovedSeries() {
        var semesterId = semester(START, END);
        var courseId = course(
                semesterId,
                series(lecture(), DayOfWeek.MONDAY, START, date(10, 26), 1),
                series(lecture(), DayOfWeek.THURSDAY, START, date(10, 26), 1)
        );
        var stored = TimetableStore.loadCourse(db, courseId).series();
        assertEquals(7, rows("session"));        // 4 Mondays and 3 Thursdays

        var monday = stored.get(0);
        var newRoom = new SessionDetails(
                SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "B2", null, lecturerId, null, SessionDetails.NO_REMINDER
        );
        var changedMonday = new Series(monday.id(), courseId, newRoom, monday.schedule());
        var added = series(lecture(), DayOfWeek.FRIDAY, START, date(10, 9), 1);
        // The Thursday series is left out, so it is removed.

        TimetableStore.saveCourse(
                db, new Course(courseId, semesterId, "  Algebra ", CourseColor.TEAL, null),
                List.of(changedMonday, added)
        );

        var reloaded = TimetableStore.loadCourse(db, courseId);
        assertEquals("Algebra", reloaded.course().name());
        assertEquals(CourseColor.TEAL, reloaded.course().color());
        assertEquals(2, reloaded.series().size());
        // 4 Mondays with the new room plus one Friday; the 3 Thursdays are gone.
        assertEquals(5, rows("session"));
        var timetable = TimetableStore.loadTimetable(db);
        assertEquals("B2", timetable.on(date(10, 12)).get(0).session().details().room());
        assertEquals(1, timetable.on(date(10, 9)).size());
        assertTrue(timetable.on(date(10, 8)).isEmpty());
    }

    @Test
    public void foreignKeys_areEnforced() {
        var orphan = new ContentValues();
        orphan.put("series_id", 4711);
        orphan.put("day", START.toEpochDay());
        orphan.put("type", "lecture");
        orphan.put("start_min", 480);
        orphan.put("end_min", 570);
        orphan.put("mode", "online");
        orphan.put("hybrid", 0);

        assertThrows(SQLiteConstraintException.class, () -> db.insertOrThrow("session", null, orphan));
    }

    @Test
    public void observe_reRunsTheQueryAfterAWrite() throws Exception {
        var database = Database.inMemory(context);
        LiveData<ArrayList<Semester>> live = database.observe(TimetableStore::listSemesters);
        var sawSemester = new CountDownLatch(1);
        Observer<ArrayList<Semester>> observer = semesters -> {
            if (semesters.size() == 1) sawSemester.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.observeForever(observer));

        database.write(writable -> TimetableStore.saveSemester(writable, new Semester(0, START, END, null)), null);

        assertTrue("Expected the live query to deliver the new semester", sawSemester.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.removeObserver(observer));
    }
}