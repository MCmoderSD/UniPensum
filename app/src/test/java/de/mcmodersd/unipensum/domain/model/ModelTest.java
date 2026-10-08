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
        return new SessionDetails(SessionType.LECTURE, 480, 570, mode, hybrid, room, link, 0, "", SessionDetails.NO_REMINDER);
    }

    @Test
    public void schedule_rejectsWeekend() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.SATURDAY, date(10, 5), date(11, 2), 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.SUNDAY, date(10, 5), date(11, 2), 1)
        );
    }

    @Test
    public void schedule_rejectsIntervalBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schedule(DayOfWeek.MONDAY, date(10, 5), date(11, 2), 0)
        );
    }

    @Test
    public void semester_containsBothBoundaries() {
        var semester = new Semester(1, date(10, 5), date(11, 2), null);
        assertTrue(semester.contains(date(10, 5)));
        assertTrue(semester.contains(date(11, 2)));
        assertFalse(semester.contains(date(10, 4)));
        assertFalse(semester.contains(date(11, 3)));
    }

    @Test
    public void normalized_online_dropsRoomAndHybridButKeepsLink() {
        var result = details(Mode.ONLINE, true, "A1", "https://meet.example/x").normalized();
        assertNull(result.room());
        assertFalse(result.hybrid());
        assertEquals("https://meet.example/x", result.link());
    }

    @Test
    public void normalized_inPersonWithoutHybrid_dropsLink() {
        var result = details(Mode.IN_PERSON, false, "A1", "https://meet.example/x").normalized();
        assertEquals("A1", result.room());
        assertNull(result.link());
    }

    @Test
    public void normalized_hybrid_keepsRoomAndLink() {
        var result = details(Mode.IN_PERSON, true, "A1", "https://meet.example/x").normalized();
        assertEquals("A1", result.room());
        assertEquals("https://meet.example/x", result.link());
        assertTrue(result.hybrid());
    }

    @Test
    public void normalized_blankTextBecomesNull() {
        var result = details(Mode.IN_PERSON, false, "  ", null).normalized();
        assertNull(result.room());
        assertNull(result.note());
    }

    @Test
    public void normalized_cleansTextAndTheLink() {
        var dirty = new SessionDetails(
                SessionType.LECTURE, 480, 570, Mode.IN_PERSON, true,
                "  A1\t ​", " meet.example/abc ", 0, " bring ‮laptop \r\n\r\n\r\n room B2 ", SessionDetails.NO_REMINDER
        );

        var result = dirty.normalized();

        assertEquals("A1", result.room());
        assertEquals("https://meet.example/abc", result.link());
        assertEquals("bring laptop\n\nroom B2", result.note());
    }

    @Test
    public void normalized_isIdempotent() {
        var once = new SessionDetails(
                SessionType.LAB, 480, 570, Mode.IN_PERSON, true,
                " A1 ", "HTTPS://meet.example/a b", 3, " x \n\n\n y ", SessionDetails.NO_REMINDER
        ).normalized();
        assertEquals(once, once.normalized());
    }

    @Test
    public void normalized_refusesALinkThatIsNoWebLink() {
        assertThrows(
                IllegalArgumentException.class,
                () -> details(Mode.ONLINE, false, null, "javascript:alert(1)").normalized()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> details(Mode.IN_PERSON, true, "A1", "ftp://files.example/x").normalized()
        );
    }

    @Test
    public void normalized_doesNotLookAtALinkThatIsDroppedAnyway() {
        assertNull(details(Mode.IN_PERSON, false, "A1", "javascript:alert(1)").normalized().link());
    }

    @Test
    public void normalized_keepsALecturerAndMapsInvalidIdsToNone() {
        var base = details(Mode.IN_PERSON, false, "A1", null);
        assertEquals(7, withLecturer(base, 7).normalized().lecturerId());
        assertEquals(SessionDetails.NO_LECTURER, withLecturer(base, -3).normalized().lecturerId());
    }

    private static SessionDetails withLecturer(SessionDetails d, long lecturerId) {
        return new SessionDetails(
                d.type(), d.startMin(), d.endMin(), d.mode(), d.hybrid(),
                d.room(), d.link(), lecturerId, d.note(), d.reminderMin()
        );
    }

    @Test
    public void normalized_bringsTheReminderIntoItsRange() {
        var base = details(Mode.IN_PERSON, false, "A1", null);
        assertEquals(30, base.withReminder(30).normalized().reminderMin());
        assertEquals(0, base.withReminder(0).normalized().reminderMin());
        assertEquals(SessionDetails.MAX_REMINDER_MIN, base.withReminder(180).normalized().reminderMin());
        assertEquals(SessionDetails.MAX_REMINDER_MIN, base.withReminder(100_000).normalized().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, base.withReminder(-1).normalized().reminderMin());
        assertEquals(SessionDetails.NO_REMINDER, base.withReminder(-30).normalized().reminderMin());
    }

    @Test
    public void reminder_isKeptForEveryFormat() {
        assertEquals(5, details(Mode.ONLINE, false, null, null).withReminder(5).normalized().reminderMin());
        assertEquals(
                45, details(Mode.IN_PERSON, true, "A1", "https://meet.example/x").withReminder(45)
                        .normalized().reminderMin()
        );
    }

    @Test
    public void hasReminder_isTrueForZeroMinutes() {
        assertTrue(details(Mode.IN_PERSON, false, null, null).withReminder(0).hasReminder());
        assertFalse(details(Mode.IN_PERSON, false, null, null).withReminder(SessionDetails.NO_REMINDER).hasReminder());
    }

    @Test
    public void with_changesOnlyThatField() {
        var base = details(Mode.ONLINE, false, null, "https://meet.example/x").withReminder(15);

        assertEquals(
                base.withLecturer(9), new SessionDetails(
                        base.type(), base.startMin(), base.endMin(), base.mode(),
                        base.hybrid(), base.room(), base.link(), 9, base.note(), 15
                )
        );
        assertEquals(15, base.withLink(null).reminderMin());
        assertNull(base.withLink(null).link());
        assertEquals("https://meet.example/x", base.withReminder(0).link());
    }

    @Test
    public void hasValidTimes() {
        assertTrue(new SessionDetails(SessionType.LAB, 0, 1440, Mode.ONLINE, false, null, null, 0, null, SessionDetails.NO_REMINDER).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 600, 600, Mode.ONLINE, false, null, null, 0, null, SessionDetails.NO_REMINDER).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 700, 600, Mode.ONLINE, false, null, null, 0, null, SessionDetails.NO_REMINDER).hasValidTimes());
        assertFalse(new SessionDetails(SessionType.LAB, 600, 1441, Mode.ONLINE, false, null, null, 0, null, SessionDetails.NO_REMINDER).hasValidTimes());
    }

    @Test
    public void lecturer_nameFollowsTheStyle() {
        var lecturer = new Lecturer(1, "Anna", "Weber", null, null);
        assertEquals("Weber", lecturer.name(NameStyle.LAST_NAME));
        assertEquals("Anna Weber", lecturer.name(NameStyle.FULL_NAME));
    }

    @Test
    public void lecturer_withoutFirstName_showsTheLastNameInBothStyles() {
        var lecturer = new Lecturer(1, "", "Prof. Weber", null, null);
        assertEquals("Prof. Weber", lecturer.name(NameStyle.LAST_NAME));
        assertEquals("Prof. Weber", lecturer.name(NameStyle.FULL_NAME));
    }

    @Test
    public void lecturer_normalized_cleansEveryField() {
        var result = new Lecturer(
                2, " Anna​\n ", "‮Weber  Koch", " anna .weber@uni.example ",
                "tel: +49 (30) 123"
        ).normalized();

        assertEquals("Anna", result.firstName());
        assertEquals("Weber Koch", result.lastName());
        assertEquals("anna.weber@uni.example", result.email());
        assertEquals("+49 (30) 123", result.phone());
    }

    @Test
    public void lecturer_normalized_isIdempotent() {
        var once = new Lecturer(2, " Anna ", " Weber ", " a@b.example ", " 030  123 ").normalized();
        assertEquals(once, once.normalized());
    }

    @Test
    public void lecturer_normalized_trimsAndTurnsBlankContactsIntoNull() {
        var result = new Lecturer(4, "  Anna ", " Weber  ", "   ", " +49 30 123 ").normalized();
        assertEquals("Anna", result.firstName());
        assertEquals("Weber", result.lastName());
        assertNull(result.email());
        assertEquals("+49 30 123", result.phone());
        assertEquals(4, result.id());
    }

    @Test
    public void keys_roundTrip() {
        for (var value : SessionType.values()) assertEquals(value, SessionType.fromKey(value.key()));
        for (var value : Mode.values()) assertEquals(value, Mode.fromKey(value.key()));
        for (var value : CourseColor.values()) assertEquals(value, CourseColor.fromKey(value.key()));
        assertThrows(IllegalArgumentException.class, () -> SessionType.fromKey("nope"));
    }
}