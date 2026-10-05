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
        return new SessionDetails(SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "A1", null, lecturerId, null);
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
        long semesterId = semester(START, END);
        course(semesterId,
                series(lecture(), DayOfWeek.MONDAY, START, END, 1),
                series(lecture(), DayOfWeek.THURSDAY, START, END, 2));

        // 19 Mondays plus 10 Thursdays in every second week.
        assertEquals(29, rows("session"));
        Timetable timetable = TimetableStore.loadTimetable(db);
        assertEquals(1, timetable.on(date(10, 5)).size());
        assertEquals("Math", timetable.on(date(10, 5)).get(0).courseName());
        assertEquals(CourseColor.BLUE, timetable.on(date(10, 5)).get(0).color());
        assertEquals(1, timetable.on(date(10, 8)).size());
        assertTrue(timetable.on(date(10, 15)).isEmpty());
        assertEquals(1, timetable.on(date(10, 22)).size());
    }

    @Test
    public void series_roundTripsEveryField() {
        long semesterId = semester(START, END);
        SessionDetails hybrid = new SessionDetails(SessionType.TUTORIAL, 13 * 60 + 15, 14 * 60 + 45,
                Mode.IN_PERSON, true, "B 2.04", "https://meet.example/room", lecturerId, "Bring laptop");
        course(semesterId, series(hybrid, DayOfWeek.WEDNESDAY, date(10, 7), date(12, 16), 3));

        Series stored = TimetableStore.listCourses(db, semesterId).get(0).series().get(0);

        assertEquals(hybrid, stored.details());
        assertEquals(new Schedule(DayOfWeek.WEDNESDAY, date(10, 7), date(12, 16), 3), stored.schedule());
    }

    @Test
    public void createCourse_normalizesDetailsForTheChosenMode() {
        long semesterId = semester(START, END);
        SessionDetails online = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.ONLINE, true,
                "Leftover room", "https://meet.example/x", SessionDetails.NO_LECTURER, "");
        course(semesterId, series(online, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1));

        SessionDetails stored = TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details();

        assertNull(stored.room());
        assertEquals(false, stored.hybrid());
        assertEquals("https://meet.example/x", stored.link());
        assertEquals(SessionDetails.NO_LECTURER, stored.lecturerId());
        assertNull(stored.note());
    }

    @Test
    public void createCourse_dropsALecturerThatNoLongerExists() {
        long semesterId = semester(START, END);
        SessionDetails gone = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.IN_PERSON, false,
                "A1", null, 4711, null);

        course(semesterId, series(gone, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1));

        assertEquals(SessionDetails.NO_LECTURER,
                TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId());
        assertEquals(1, rows("session"));
    }

    // --- lecturers ---

    @Test
    public void lecturers_areSavedUpdatedAndListedByLastName() {
        long weber = TimetableStore.saveLecturer(db, new Lecturer(0, " Anna ", "Weber", " ", "  +49 1 "));
        long adler = TimetableStore.saveLecturer(db, new Lecturer(0, "", "adler", "adler@uni.example", null));

        TimetableStore.saveLecturer(db, new Lecturer(weber, "Anna", "Weber-Koch", "a@uni.example", null));

        List<Lecturer> all = TimetableStore.listLecturers(db);
        // The one from setUp is "Example"; the list is ordered by last name regardless of case.
        assertEquals(List.of("adler", "Example", "Weber-Koch"),
                all.stream().map(Lecturer::lastName).collect(Collectors.toList()));
        Lecturer updated = all.get(2);
        assertEquals(weber, updated.id());
        assertEquals("a@uni.example", updated.email());
        assertNull(updated.phone());
        assertEquals(adler, all.get(0).id());
        assertEquals("", all.get(0).firstName());
    }

    @Test
    public void saveLecturer_trimsAndTurnsBlankContactsIntoNull() {
        long id = TimetableStore.saveLecturer(db, new Lecturer(0, " Anna ", " Weber ", " ", " +49 1 "));

        Lecturer stored = TimetableStore.listLecturers(db).stream().filter(l -> l.id() == id).findFirst().orElseThrow();
        assertEquals("Anna", stored.firstName());
        assertEquals("Weber", stored.lastName());
        assertNull(stored.email());
        assertEquals("+49 1", stored.phone());
    }

    @Test
    public void saveLecturer_requiresALastNameAndAKnownId() {
        assertThrows(IllegalArgumentException.class,
                () -> TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "  ", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> TimetableStore.saveLecturer(db, new Lecturer(4711, "Anna", "Weber", null, null)));
    }

    @Test
    public void timetableAndSessionContext_carryTheLecturer() {
        long semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, date(10, 12), 1));

        SessionView view = TimetableStore.loadTimetable(db).on(date(10, 5)).get(0);
        SessionContext context = TimetableStore.loadSessionContext(db, view.session().id());

        assertEquals("Example", view.lecturer().lastName());
        assertEquals("anna@uni.example", context.lecturer().email());
    }

    @Test
    public void sessionWithoutALecturer_hasNone() {
        long semesterId = semester(START, END);
        SessionDetails none = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.IN_PERSON, false,
                "A1", null, SessionDetails.NO_LECTURER, null);
        course(semesterId, series(none, DayOfWeek.MONDAY, START, START, 1));

        SessionView view = TimetableStore.loadTimetable(db).on(START).get(0);

        assertNull(view.lecturer());
        assertNull(TimetableStore.loadSessionContext(db, view.session().id()).lecturer());
    }

    @Test
    public void deleteLecturer_leavesTheirSessionsWithoutOne() {
        long semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, date(10, 12), 1));
        assertEquals(2, rows("session"));

        TimetableStore.deleteLecturer(db, lecturerId);

        assertEquals(2, rows("session"));
        assertEquals(1, rows("series"));
        assertEquals(SessionDetails.NO_LECTURER,
                TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId());
        assertNull(TimetableStore.loadTimetable(db).on(START).get(0).lecturer());
        assertEquals(SessionDetails.NO_LECTURER,
                TimetableStore.loadTimetable(db).on(START).get(0).session().details().lecturerId());
    }

    @Test
    public void editSession_changingTheLecturer_reachesTheWholeSeriesOrOneSession() {
        long semesterId = semester(START, END);
        Schedule schedule = new Schedule(DayOfWeek.MONDAY, START, date(10, 26), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));
        long other = TimetableStore.saveLecturer(db, new Lecturer(0, "Max", "Other", null, null));
        SessionDetails withOther = new SessionDetails(SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "A1", null, other, null);

        TimetableStore.editSession(db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY, withOther, date(10, 12), null);
        Timetable timetable = TimetableStore.loadTimetable(db);
        assertEquals("Other", timetable.on(date(10, 12)).get(0).lecturer().lastName());
        assertEquals("Example", timetable.on(date(10, 5)).get(0).lecturer().lastName());

        TimetableStore.editSession(db, sessionIdOn(date(10, 5)), EditScope.ALL, withOther, null, schedule);
        timetable = TimetableStore.loadTimetable(db);
        for (int day : new int[]{5, 12, 19, 26}) {
            assertEquals("Other", timetable.on(date(10, day)).get(0).lecturer().lastName());
        }
        assertEquals(other, TimetableStore.listCourses(db, semesterId).get(0).series().get(0).details().lecturerId());
    }

    // --- cleaning of typed text ---

    @Test
    public void createCourse_cleansTheNameTheLinksAndTheNotes() {
        long semesterId = semester(START, END);
        SessionDetails dirty = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, " meet.example/x ", lecturerId, "  a \r\n\r\n\r\n b​ ");

        long courseId = TimetableStore.createCourse(db,
                new Course(0, semesterId, "  Math​ \t II ", CourseColor.BLUE, " moodle.example/c/1 "),
                List.of(series(dirty, DayOfWeek.FRIDAY, date(10, 9), date(10, 9), 1)));

        CourseContext stored = TimetableStore.loadCourse(db, courseId);
        assertEquals("Math II", stored.course().name());
        assertEquals("https://moodle.example/c/1", stored.course().moodleLink());
        assertEquals("https://meet.example/x", stored.series().get(0).details().link());
        assertEquals("a\n\nb", stored.series().get(0).details().note());
    }

    @Test
    public void createCourse_refusesALinkThatIsNoWebLinkAndStoresNothing() {
        long semesterId = semester(START, END);

        assertThrows(IllegalArgumentException.class, () -> TimetableStore.createCourse(db,
                new Course(0, semesterId, "Math", CourseColor.BLUE, "javascript:alert(1)"),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1))));
        SessionDetails badMeeting = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, "ftp://files.example/x", lecturerId, null);
        assertThrows(IllegalArgumentException.class, () -> course(semesterId,
                series(badMeeting, DayOfWeek.MONDAY, START, START, 1)));

        assertEquals(0, rows("course"));
        assertEquals(0, rows("series"));
    }

    @Test
    public void createCourse_refusesANameOfOnlyInvisibleCharacters() {
        long semesterId = semester(START, END);
        assertThrows(IllegalArgumentException.class, () -> TimetableStore.createCourse(db,
                new Course(0, semesterId, " ​‮\t ", CourseColor.BLUE, null),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1))));
        assertEquals(0, rows("course"));
    }

    @Test
    public void saveSemester_cleansTheCustomNameAndTurnsBlankIntoNone() {
        long id = TimetableStore.saveSemester(db, new Semester(0, START, END, "  Winter​ \n term "));
        assertEquals("Winter term", TimetableStore.listSemesters(db).get(0).customName());

        TimetableStore.saveSemester(db, new Semester(id, START, END, " ​ "));
        assertNull(TimetableStore.listSemesters(db).get(0).customName());
    }

    @Test
    public void saveCourse_doesNotRejectAnUntouchedEventBecauseOfAnOldLink() {
        long semesterId = semester(START, END);
        SessionDetails online = new SessionDetails(SessionType.LECTURE, 600, 700, Mode.ONLINE, false,
                null, "https://meet.example/x", lecturerId, null);
        long courseId = course(semesterId, series(online, DayOfWeek.MONDAY, START, date(10, 12), 1));
        // A link an earlier version accepted and today's rules would refuse.
        db.execSQL("UPDATE series SET link = 'ftp://old.example/x'");
        CourseContext stored = TimetableStore.loadCourse(db, courseId);

        TimetableStore.saveCourse(db, new Course(courseId, semesterId, "Renamed", CourseColor.TEAL, null),
                stored.series());

        CourseContext reloaded = TimetableStore.loadCourse(db, courseId);
        assertEquals("Renamed", reloaded.course().name());
        assertEquals("ftp://old.example/x", reloaded.series().get(0).details().link());
    }

    // --- moodle link ---

    @Test
    public void moodleLink_isStoredTrimmedAndKeptWhenTheCourseIsSaved() {
        long semesterId = semester(START, END);
        long courseId = TimetableStore.createCourse(db,
                new Course(0, semesterId, "Math", CourseColor.BLUE, "  https://moodle.example/c/1 "),
                List.of(series(lecture(), DayOfWeek.MONDAY, START, START, 1)));
        assertEquals("https://moodle.example/c/1", TimetableStore.loadCourse(db, courseId).course().moodleLink());

        TimetableStore.saveCourse(db, new Course(courseId, semesterId, "Math", CourseColor.BLUE, "https://moodle.example/c/2"),
                TimetableStore.loadCourse(db, courseId).series());
        assertEquals("https://moodle.example/c/2", TimetableStore.loadCourse(db, courseId).course().moodleLink());

        TimetableStore.saveCourse(db, new Course(courseId, semesterId, "Math", CourseColor.BLUE, "   "),
                TimetableStore.loadCourse(db, courseId).series());
        assertNull(TimetableStore.loadCourse(db, courseId).course().moodleLink());
    }

    @Test
    public void createCourse_rejectsScheduleOutsideTheSemester() {
        long semesterId = semester(START, END);
        assertThrows(IllegalArgumentException.class, () -> course(semesterId,
                series(lecture(), DayOfWeek.MONDAY, date(9, 28), date(10, 26), 1)));
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
        long semesterId = semester(START, date(11, 13));
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
        long semesterId = semester(START, END);
        course(semesterId, series(lecture(), DayOfWeek.MONDAY, START, END, 1));

        TimetableStore.deleteSemester(db, semesterId);

        assertEquals(0, rows("course"));
        assertEquals(0, rows("series"));
        assertEquals(0, rows("session"));
    }

    @Test
    public void editSession_thisAndFollowing_splitsTheSeriesInTheDatabase() {
        long semesterId = semester(START, END);
        Schedule schedule = new Schedule(DayOfWeek.MONDAY, START, date(11, 2), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));
        long third = sessionIdOn(date(10, 19));
        SessionDetails moved = new SessionDetails(SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "B2", null, lecturerId, null);

        TimetableStore.editSession(db, third, EditScope.THIS_AND_FOLLOWING, moved, date(10, 19), schedule);

        List<Series> series = TimetableStore.listCourses(db, semesterId).get(0).series();
        assertEquals(2, series.size());
        assertEquals(5, rows("session"));
        Timetable timetable = TimetableStore.loadTimetable(db);
        assertEquals("A1", timetable.on(date(10, 12)).get(0).session().details().room());
        assertEquals("B2", timetable.on(date(10, 19)).get(0).session().details().room());
        assertEquals("B2", timetable.on(date(11, 2)).get(0).session().details().room());
        // Both halves reference their own series.
        long firstHalf = timetable.on(date(10, 12)).get(0).session().seriesId();
        long secondHalf = timetable.on(date(10, 19)).get(0).session().seriesId();
        assertTrue(firstHalf != secondHalf);
        assertEquals(secondHalf, timetable.on(date(11, 2)).get(0).session().seriesId());
    }

    @Test
    public void editSession_thisOnly_movesJustOneSession() {
        long semesterId = semester(START, END);
        Schedule schedule = new Schedule(DayOfWeek.MONDAY, START, date(10, 26), 1);
        course(semesterId, new Series(0, 0, lecture(), schedule));

        TimetableStore.editSession(db, sessionIdOn(date(10, 12)), EditScope.THIS_ONLY, lecture(), date(10, 14), null);

        Timetable timetable = TimetableStore.loadTimetable(db);
        assertTrue(timetable.on(date(10, 12)).isEmpty());
        assertEquals(1, timetable.on(date(10, 14)).size());
        assertEquals(4, rows("session"));
        assertEquals(1, rows("series"));
    }

    @Test
    public void deleteSession_scopes() {
        long semesterId = semester(START, END);
        Schedule schedule = new Schedule(DayOfWeek.MONDAY, START, date(11, 2), 1);
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
        long semesterId = semester(START, END);
        long courseId = course(semesterId,
                series(lecture(), DayOfWeek.MONDAY, START, date(10, 26), 1),
                series(lecture(), DayOfWeek.THURSDAY, START, date(10, 26), 1));
        List<Series> stored = TimetableStore.loadCourse(db, courseId).series();
        assertEquals(7, rows("session"));        // 4 Mondays and 3 Thursdays

        Series monday = stored.get(0);
        SessionDetails newRoom = new SessionDetails(SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "B2", null, lecturerId, null);
        Series changedMonday = new Series(monday.id(), courseId, newRoom, monday.schedule());
        Series added = series(lecture(), DayOfWeek.FRIDAY, START, date(10, 9), 1);
        // The Thursday series is left out, so it is removed.

        TimetableStore.saveCourse(db, new Course(courseId, semesterId, "  Algebra ", CourseColor.TEAL, null),
                List.of(changedMonday, added));

        CourseContext reloaded = TimetableStore.loadCourse(db, courseId);
        assertEquals("Algebra", reloaded.course().name());
        assertEquals(CourseColor.TEAL, reloaded.course().color());
        assertEquals(2, reloaded.series().size());
        // 4 Mondays with the new room plus one Friday; the 3 Thursdays are gone.
        assertEquals(5, rows("session"));
        Timetable timetable = TimetableStore.loadTimetable(db);
        assertEquals("B2", timetable.on(date(10, 12)).get(0).session().details().room());
        assertEquals(1, timetable.on(date(10, 9)).size());
        assertTrue(timetable.on(date(10, 8)).isEmpty());
    }

    @Test
    public void foreignKeys_areEnforced() {
        ContentValues orphan = new ContentValues();
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
        Database database = Database.inMemory(context);
        LiveData<List<Semester>> live = database.observe(TimetableStore::listSemesters);
        CountDownLatch sawSemester = new CountDownLatch(1);
        Observer<List<Semester>> observer = semesters -> {
            if (semesters.size() == 1) sawSemester.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.observeForever(observer));

        database.write(writable -> TimetableStore.saveSemester(writable, new Semester(0, START, END, null)), null);

        assertTrue("Expected the live query to deliver the new semester", sawSemester.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.removeObserver(observer));
    }
}
