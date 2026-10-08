package de.mcmodersd.unipensum.domain.backup;

import static de.mcmodersd.unipensum.domain.Fixtures.LECTURER;
import static de.mcmodersd.unipensum.domain.Fixtures.SEMESTER;
import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static de.mcmodersd.unipensum.domain.Fixtures.details;
import static de.mcmodersd.unipensum.domain.Fixtures.sessionsOf;
import static de.mcmodersd.unipensum.domain.Fixtures.weekly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

public class BackupCleanerTest {

    private static final Lecturer WEBER = new Lecturer(LECTURER, "Anna", "Weber", "anna@uni.example", "030 1234");
    private static final Course COURSE = new Course(1, 1, "Math", CourseColor.BLUE, "https://moodle.example/c/1");
    private static final Series SERIES = new Series(10, 1, details(), weekly(DayOfWeek.MONDAY, date(10, 5), date(10, 26)));

    /** One lecturer, one semester, one course, one series with its four Monday sessions. */
    private static BackupData good() {
        return new BackupData(List.of(WEBER), List.of(SEMESTER), List.of(COURSE), List.of(SERIES),
                sessionsOf(SERIES, 100));
    }

    private static BackupData with(BackupData base, List<Lecturer> lecturers, List<Semester> semesters,
                                   List<Course> courses, List<Series> series, List<Session> sessions) {
        return new BackupData(lecturers != null ? lecturers : base.lecturers(),
                semesters != null ? semesters : base.semesters(),
                courses != null ? courses : base.courses(),
                series != null ? series : base.series(),
                sessions != null ? sessions : base.sessions());
    }

    private static BackupCleaner.Result clean(BackupData data) {
        return BackupCleaner.clean(data, 0, 0);
    }

    private static SessionDetails withLink(SessionDetails d, Mode mode, String link) {
        return new SessionDetails(d.type(), d.startMin(), d.endMin(), mode, d.hybrid(), d.room(), link,
                d.lecturerId(), d.note(), d.reminderMin());
    }

    private static SessionDetails withTimes(SessionDetails d, int start, int end) {
        return new SessionDetails(d.type(), start, end, d.mode(), d.hybrid(), d.room(), d.link(),
                d.lecturerId(), d.note(), d.reminderMin());
    }

    @Test
    public void cleanData_isTakenOverAsItIs() {
        var result = clean(good());

        assertEquals(good(), result.data());
        assertEquals(new BackupReport(1, 1, 1, 1, 4, 0, 0), result.report());
        assertTrue(result.report().isClean());
    }

    @Test
    public void whatTheReaderSkippedOrChanged_isAddedToTheReport() {
        var report = BackupCleaner.clean(good(), 3, 2).report();

        assertEquals(3, report.skipped());
        assertEquals(2, report.adjusted());
    }

    @Test
    public void texts_areCleaned() {
        var course = new Course(1, 1, "  Math​ \t II ", CourseColor.BLUE, " moodle.example/c/1 ");
        var semester = new Semester(1, SEMESTER.start(), SEMESTER.end(), " Winter‮  term ");
        var lecturer = new Lecturer(LECTURER, " Anna​", " Weber ", " a@uni.example ", "tel 030");

        var data = clean(with(good(), List.of(lecturer), List.of(semester), List.of(course), null, null)).data();

        assertEquals("Math II", data.courses().get(0).name());
        assertEquals("https://moodle.example/c/1", data.courses().get(0).moodleLink());
        assertEquals("Winter term", data.semesters().get(0).customName());
        assertEquals("Anna", data.lecturers().get(0).firstName());
        assertEquals("a@uni.example", data.lecturers().get(0).email());
        assertEquals("030", data.lecturers().get(0).phone());
    }

    @Test
    public void reminders_areKeptAndBroughtIntoTheirRange() {
        var series = new Series(10, 1, SERIES.details().withReminder(45), SERIES.schedule());
        var tooLong = new Series(11, 1, SERIES.details().withReminder(5_000), SERIES.schedule());
        var negative = new Series(12, 1, SERIES.details().withReminder(-9), SERIES.schedule());

        var data = clean(with(good(), null, null, null, List.of(series, tooLong, negative), List.of())).data();

        assertEquals(45, data.series().get(0).details().reminderMin());
        assertEquals(SessionDetails.MAX_REMINDER_MIN, data.series().get(1).details().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, data.series().get(2).details().reminderMin());
    }

    // --- lecturers ---

    @Test
    public void lecturerWithoutALastName_isSkipped_andClearedFromTheEventsThatUsedIt() {
        var nameless = new Lecturer(LECTURER, "Anna", " ​ ", null, null);

        var result = clean(with(good(), List.of(nameless), null, null, null, null));

        assertTrue(result.data().lecturers().isEmpty());
        assertEquals(SessionDetails.NO_LECTURER, result.data().series().get(0).details().lecturerId());
        for (var session : result.data().sessions()) {
            assertEquals(SessionDetails.NO_LECTURER, session.details().lecturerId());
        }
        assertEquals(1, result.report().skipped());
        assertEquals(5, result.report().adjusted());          // the series and its four sessions
    }

    @Test
    public void aLecturerThatDoesNotExist_isCleared() {
        var result = clean(with(good(), List.of(), null, null, null, null));

        assertEquals(SessionDetails.NO_LECTURER, result.data().series().get(0).details().lecturerId());
        assertEquals(0, result.report().skipped());
        assertEquals(5, result.report().adjusted());
    }

    // --- semesters ---

    @Test
    public void semesterThatEndsBeforeItBegins_isSkippedWithEverythingInIt() {
        var broken = new Semester(1, date(11, 1), date(10, 1), null);

        var result = clean(with(good(), null, List.of(broken), null, null, null));

        assertTrue(result.data().semesters().isEmpty());
        assertTrue(result.data().courses().isEmpty());
        assertTrue(result.data().series().isEmpty());
        assertTrue(result.data().sessions().isEmpty());
        assertEquals(1 + 1 + 1 + 4, result.report().skipped());
    }

    @Test
    public void overlappingSemester_isSkipped_theEarlierOneWins() {
        var later = new Semester(2, date(12, 1), LocalDate.of(2027, 3, 1), null);
        var laterCourse = new Course(2, 2, "Physics", CourseColor.RED, null);
        var laterSeries = new Series(20, 2, details(),
                weekly(DayOfWeek.TUESDAY, date(12, 1), date(12, 15)));
        List<Semester> semesters = List.of(later, SEMESTER);               // the order in the file does not matter

        var data = new BackupData(List.of(WEBER), semesters, List.of(COURSE, laterCourse),
                List.of(SERIES, laterSeries), sessionsOf(SERIES, 100));
        var result = clean(data);

        assertEquals(List.of(SEMESTER), result.data().semesters());
        assertEquals(List.of(COURSE), result.data().courses());
        assertEquals(List.of(SERIES), result.data().series());
        assertEquals(3, result.report().skipped());                         // semester, course, series
    }

    // --- courses ---

    @Test
    public void courseWithoutAUsableName_isSkippedWithItsEvents() {
        var nameless = new Course(1, 1, "​ \t", CourseColor.BLUE, null);

        var result = clean(with(good(), null, null, List.of(nameless), null, null));

        assertTrue(result.data().courses().isEmpty());
        assertTrue(result.data().series().isEmpty());
        assertTrue(result.data().sessions().isEmpty());
        assertEquals(1 + 1 + 4, result.report().skipped());
    }

    @Test
    public void courseWithoutASemester_isSkipped() {
        var orphan = new Course(1, 99, "Math", CourseColor.BLUE, null);

        var result = clean(with(good(), null, null, List.of(orphan), null, null));

        assertTrue(result.data().courses().isEmpty());
    }

    @Test
    public void aMoodleLinkThatIsNoWebLink_isDropped() {
        var course = new Course(1, 1, "Math", CourseColor.BLUE, "javascript:alert(1)");

        var result = clean(with(good(), null, null, List.of(course), null, null));

        assertNull(result.data().courses().get(0).moodleLink());
        assertEquals(1, result.report().adjusted());
        assertEquals(0, result.report().skipped());
    }

    // --- events and sessions ---

    @Test
    public void eventOutsideItsSemester_isSkippedWithItsSessions() {
        var outside = new Series(10, 1, details(), weekly(DayOfWeek.MONDAY, date(9, 7), date(10, 26)));

        var result = clean(with(good(), null, null, null, List.of(outside), null));

        assertTrue(result.data().series().isEmpty());
        assertTrue(result.data().sessions().isEmpty());
        assertEquals(1 + 4, result.report().skipped());
    }

    @Test
    public void eventWithImpossibleTimes_isSkipped() {
        var backwards = new Series(10, 1, withTimes(details(), 600, 500), SERIES.schedule());

        var result = clean(with(good(), null, null, null, List.of(backwards), null));

        assertTrue(result.data().series().isEmpty());
    }

    @Test
    public void sessionOnAWeekend_orOutsideTheSemester_orWithoutAnEvent_isSkipped() {
        var sessions = new ArrayList<Session>(sessionsOf(SERIES, 100));
        sessions.add(new Session(200, 10, date(10, 10), details()));                       // Saturday
        sessions.add(new Session(201, 10, LocalDate.of(2027, 3, 1), details()));           // after the semester
        sessions.add(new Session(202, 77, date(10, 12), details()));                       // no such event

        var result = clean(with(good(), null, null, null, null, sessions));

        assertEquals(4, result.data().sessions().size());
        assertEquals(3, result.report().skipped());
    }

    @Test
    public void sessionWithImpossibleTimes_isSkipped() {
        var broken = new Session(200, 10, date(10, 12), withTimes(details(), 700, 700));
        var sessions = new ArrayList<Session>(sessionsOf(SERIES, 100));
        sessions.add(broken);

        var result = clean(with(good(), null, null, null, null, sessions));

        assertEquals(4, result.data().sessions().size());
        assertEquals(1, result.report().skipped());
    }

    @Test
    public void aMeetingLinkThatIsNoWebLink_isDropped_theEventStays() {
        var online = withLink(details(), Mode.ONLINE, "ftp://files.example/x");
        var series = new Series(10, 1, online, SERIES.schedule());
        var session = new Session(100, 10, date(10, 5), online);

        var result = clean(with(good(), null, null, null, List.of(series), List.of(session)));

        assertNull(result.data().series().get(0).details().link());
        assertNull(result.data().sessions().get(0).details().link());
        assertEquals(2, result.report().adjusted());
        assertEquals(0, result.report().skipped());
    }

    @Test
    public void aMeetingLinkWithoutAScheme_getsHttps() {
        var online = withLink(details(), Mode.ONLINE, "meet.example/abc");
        var series = new Series(10, 1, online, SERIES.schedule());

        var result = clean(with(good(), null, null, null, List.of(series), List.of()));

        assertEquals("https://meet.example/abc", result.data().series().get(0).details().link());
        assertEquals(0, result.report().adjusted());
    }

    // --- ids ---

    @Test
    public void entriesWithAnIdUsedBefore_orBelowOne_areSkipped() {
        var duplicate = new Lecturer(LECTURER, "Other", "Person", null, null);
        var zero = new Lecturer(0, "Zero", "Id", null, null);
        var sessions = new ArrayList<Session>(sessionsOf(SERIES, 100));
        sessions.add(new Session(100, 10, date(10, 12), details()));                       // id 100 again

        var data = new BackupData(List.of(WEBER, duplicate, zero), List.of(SEMESTER), List.of(COURSE),
                List.of(SERIES), sessions);
        var result = clean(data);

        assertEquals(List.of(WEBER), result.data().lecturers());
        assertEquals(4, result.data().sessions().size());
        assertEquals(3, result.report().skipped());
    }
}
