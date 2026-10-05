package de.mcmodersd.unipensum.domain.model;

import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;

public class ModelTest {

    private static SessionDetails details(Mode mode, boolean hybrid, String room, String link) {
        return new SessionDetails(SessionType.LECTURE, 480, 570, mode, hybrid, room, link, 0, "");
    }

    @Test
    public void schedule_rejectsWeekend() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.SATURDAY, date(10, 5), date(11, 2), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.SUNDAY, date(10, 5), date(11, 2), 1));
    }

    @Test
    public void schedule_rejectsIntervalBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.MONDAY, date(10, 5), date(11, 2), 0));
    }

    @Test
    public void semester_containsBothBoundaries() {
        Semester semester = new Semester(1, date(10, 5), date(11, 2), null);
        assertTrue(semester.contains(date(10, 5)));
        assertTrue(semester.contains(date(11, 2)));
        assertFalse(semester.contains(date(10, 4)));
        assertFalse(semester.contains(date(11, 3)));
    }

    @Test
    public void normalized_online_dropsRoomAndHybridButKeepsLink() {
        SessionDetails result = details(Mode.ONLINE, true, "A1", "https://meet.example/x").normalized();
        assertNull(result.room());
        assertFalse(result.hybrid());
        assertEquals("https://meet.example/x", result.link());
    }

    @Test
    public void normalized_inPersonWithoutHybrid_dropsLink() {
        SessionDetails result = details(Mode.IN_PERSON, false, "A1", "https://meet.example/x").normalized();
        assertEquals("A1", result.room());
        assertNull(result.link());
    }

    @Test
    public void normalized_hybrid_keepsRoomAndLink() {
        SessionDetails result = details(Mode.IN_PERSON, true, "A1", "https://meet.example/x").normalized();
        assertEquals("A1", result.room());
        assertEquals("https://meet.example/x", result.link());
        assertTrue(result.hybrid());
    }

    @Test
    public void normalized_blankTextBecomesNull() {
        SessionDetails result = details(Mode.IN_PERSON, false, "  ", null).normalized();
        assertNull(result.room());
        assertNull(result.note());
    }

    @Test
    public void normalized_cleansTextAndTheLink() {
        SessionDetails dirty = new SessionDetails(SessionType.LECTURE, 480, 570, Mode.IN_PERSON, true,
                "  A1\t ​", " meet.example/abc ", 0, " bring ‮laptop \r\n\r\n\r\n room B2 ");

        SessionDetails result = dirty.normalized();

        assertEquals("A1", result.room());
        assertEquals("https://meet.example/abc", result.link());
        assertEquals("bring laptop\n\nroom B2", result.note());
    }

    @Test
    public void normalized_isIdempotent() {
        SessionDetails once = new SessionDetails(SessionType.LAB, 480, 570, Mode.IN_PERSON, true,
                " A1 ", "HTTPS://meet.example/a b", 3, " x \n\n\n y ").normalized();
        assertEquals(once, once.normalized());
    }

    @Test
    public void normalized_refusesALinkThatIsNoWebLink() {
        assertThrows(IllegalArgumentException.class,
                () -> details(Mode.ONLINE, false, null, "javascript:alert(1)").normalized());
        assertThrows(IllegalArgumentException.class,
                () -> details(Mode.IN_PERSON, true, "A1", "ftp://files.example/x").normalized());
    }

    @Test
    public void normalized_doesNotLookAtALinkThatIsDroppedAnyway() {
        assertNull(details(Mode.IN_PERSON, false, "A1", "javascript:alert(1)").normalized().link());
    }

    @Test
    public void normalized_keepsALecturerAndMapsInvalidIdsToNone() {
        SessionDetails base = details(Mode.IN_PERSON, false, "A1", null);
        assertEquals(7, withLecturer(base, 7).normalized().lecturerId());
        assertEquals(SessionDetails.NO_LECTURER, withLecturer(base, -3).normalized().lecturerId());
    }

    private static SessionDetails withLecturer(SessionDetails d, long lecturerId) {
        return new SessionDetails(d.type(), d.startMin(), d.endMin(), d.mode(), d.hybrid(),
                d.room(), d.link(), lecturerId, d.note());
    }

    @Test
    public void hasValidTimes() {
        assertTrue(new SessionDetails(SessionType.LAB, 0, 1440, Mode.ONLINE, false, null, null, 0, null).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 600, 600, Mode.ONLINE, false, null, null, 0, null).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 700, 600, Mode.ONLINE, false, null, null, 0, null).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 600, 1441, Mode.ONLINE, false, null, null, 0, null).hasValidTimes());
    }

    @Test
    public void lecturer_nameFollowsTheStyle() {
        Lecturer lecturer = new Lecturer(1, "Anna", "Weber", null, null);
        assertEquals("Weber", lecturer.name(NameStyle.LAST_NAME));
        assertEquals("Anna Weber", lecturer.name(NameStyle.FULL_NAME));
    }

    @Test
    public void lecturer_withoutFirstName_showsTheLastNameInBothStyles() {
        Lecturer lecturer = new Lecturer(1, "", "Prof. Weber", null, null);
        assertEquals("Prof. Weber", lecturer.name(NameStyle.LAST_NAME));
        assertEquals("Prof. Weber", lecturer.name(NameStyle.FULL_NAME));
    }

    @Test
    public void lecturer_normalized_cleansEveryField() {
        Lecturer result = new Lecturer(2, " Anna​\n ", "‮Weber  Koch", " anna .weber@uni.example ",
                "tel: +49 (30) 123").normalized();

        assertEquals("Anna", result.firstName());
        assertEquals("Weber Koch", result.lastName());
        assertEquals("anna.weber@uni.example", result.email());
        assertEquals("+49 (30) 123", result.phone());
    }

    @Test
    public void lecturer_normalized_isIdempotent() {
        Lecturer once = new Lecturer(2, " Anna ", " Weber ", " a@b.example ", " 030  123 ").normalized();
        assertEquals(once, once.normalized());
    }

    @Test
    public void lecturer_normalized_trimsAndTurnsBlankContactsIntoNull() {
        Lecturer result = new Lecturer(4, "  Anna ", " Weber  ", "   ", " +49 30 123 ").normalized();
        assertEquals("Anna", result.firstName());
        assertEquals("Weber", result.lastName());
        assertNull(result.email());
        assertEquals("+49 30 123", result.phone());
        assertEquals(4, result.id());
    }

    @Test
    public void keys_roundTrip() {
        for (SessionType value : SessionType.values()) assertEquals(value, SessionType.fromKey(value.key()));
        for (Mode value : Mode.values()) assertEquals(value, Mode.fromKey(value.key()));
        for (CourseColor value : CourseColor.values()) assertEquals(value, CourseColor.fromKey(value.key()));
        assertThrows(IllegalArgumentException.class, () -> SessionType.fromKey("nope"));
    }
}
