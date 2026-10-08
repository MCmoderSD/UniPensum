package de.mcmodersd.unipensum.domain.logic;

import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static de.mcmodersd.unipensum.domain.Fixtures.details;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;

/** Fixture sessions are 08:00 to 11:00. */
public class RemindersTest {

    private static LocalDateTime at(int month, int day, int hour, int minute) {
        return LocalDateTime.of(date(month, day), java.time.LocalTime.of(hour, minute));
    }

    private static Session session(long id, int month, int day, int reminderMin) {
        return new Session(id, 10, date(month, day), details().withReminder(reminderMin));
    }

    // --- defaults ---

    @Test
    public void defaultFor_isShorterForAnOnlyOnlineEvent() {
        assertEquals(30, Reminders.defaultFor(Mode.IN_PERSON));
        assertEquals(5, Reminders.defaultFor(Mode.ONLINE));
    }

    @Test
    public void afterModeChange_movesTheDefaultAlong() {
        assertEquals(5, Reminders.afterModeChange(30, Mode.IN_PERSON, Mode.ONLINE));
        assertEquals(30, Reminders.afterModeChange(5, Mode.ONLINE, Mode.IN_PERSON));
    }

    @Test
    public void afterModeChange_keepsATimeTheUserChose() {
        assertEquals(45, Reminders.afterModeChange(45, Mode.IN_PERSON, Mode.ONLINE));
        assertEquals(10, Reminders.afterModeChange(10, Mode.ONLINE, Mode.IN_PERSON));
        // 5 is the default online, but a chosen 5 on site is not the default of that format.
        assertEquals(5, Reminders.afterModeChange(5, Mode.IN_PERSON, Mode.ONLINE));
    }

    @Test
    public void afterModeChange_keepsNoReminder() {
        assertEquals(
                SessionDetails.NO_REMINDER,
                Reminders.afterModeChange(SessionDetails.NO_REMINDER, Mode.IN_PERSON, Mode.ONLINE)
        );
    }

    // --- one reminder ---

    @Test
    public void of_isTheStartMinusTheTime() {
        var due = Reminders.of(session(1, 10, 5, 30)).orElseThrow();

        assertEquals(at(10, 5, 7, 30), due.remindAt());
        assertEquals(at(10, 5, 8, 0), due.start());
        assertEquals(at(10, 5, 11, 0), due.end());
    }

    @Test
    public void of_withZeroMinutes_isAtTheStart() {
        assertEquals(at(10, 5, 8, 0), Reminders.of(session(1, 10, 5, 0)).orElseThrow().remindAt());
    }

    @Test
    public void of_withoutAReminder_isEmpty() {
        assertTrue(Reminders.of(session(1, 10, 5, SessionDetails.NO_REMINDER)).isEmpty());
    }

    @Test
    public void of_forAnEarlySession_fallsOnTheDayBefore() {
        var early = new SessionDetails(details().type(), 10, 70, Mode.IN_PERSON, false, null, null, 0, null, 30);

        var due = Reminders.of(new Session(1, 10, date(10, 5), early)).orElseThrow();

        assertEquals(at(10, 4, 23, 40), due.remindAt());
    }

    // --- due ---

    @Test
    public void due_holdsTheRemindersBetweenTheTwoTimes() {
        List<Session> sessions = List.of(session(1, 10, 5, 30), session(2, 10, 6, 30));

        var due = Reminders.due(sessions, at(10, 5, 7, 0), at(10, 5, 7, 45));

        assertEquals(1, due.size());
        assertEquals(1, due.get(0).session().id());
    }

    @Test
    public void due_includesTheEndAndExcludesTheStartOfTheWindow() {
        List<Session> sessions = List.of(session(1, 10, 5, 30));

        assertEquals(1, Reminders.due(sessions, at(10, 5, 7, 0), at(10, 5, 7, 30)).size());
        assertTrue(Reminders.due(sessions, at(10, 5, 7, 30), at(10, 5, 7, 45)).isEmpty());
    }

    @Test
    public void due_nothingBeforeTheTime() {
        assertTrue(Reminders.due(List.of(session(1, 10, 5, 30)), at(10, 5, 6, 0), at(10, 5, 7, 29)).isEmpty());
    }

    @Test
    public void due_aMissedReminderCountsWhileTheSessionIsOn() {
        List<Session> sessions = List.of(session(1, 10, 5, 30));

        assertEquals(1, Reminders.due(sessions, at(10, 5, 7, 0), at(10, 5, 9, 15)).size());
    }

    @Test
    public void due_aMissedReminderIsDroppedOnceTheSessionIsOver() {
        List<Session> sessions = List.of(session(1, 10, 5, 30));

        assertTrue(Reminders.due(sessions, at(10, 5, 7, 0), at(10, 5, 11, 0)).isEmpty());
        assertTrue(Reminders.due(sessions, at(10, 4, 20, 0), at(10, 6, 9, 0)).isEmpty());
    }

    @Test
    public void due_skipsSessionsWithoutAReminder() {
        List<Session> sessions = List.of(session(1, 10, 5, SessionDetails.NO_REMINDER));

        assertTrue(Reminders.due(sessions, at(10, 5, 0, 0), at(10, 5, 9, 0)).isEmpty());
    }

    @Test
    public void due_isOrderedByTimeAndThenById() {
        // 07:30, 07:50, 07:50 and 07:00, all on the same day and still on at 07:55.
        List<Session> sessions = List.of(
                session(3, 10, 5, 30), session(2, 10, 5, 10), session(1, 10, 5, 10),
                session(4, 10, 5, 60)
        );

        var due = Reminders.due(sessions, at(10, 4, 0, 0), at(10, 5, 7, 55));

        assertEquals(List.of(4L, 3L, 1L, 2L), due.stream().map(d -> d.session().id()).toList());
    }

    // --- next ---

    @Test
    public void next_isTheEarliestReminderAfterNow() {
        List<Session> sessions = List.of(session(1, 10, 6, 30), session(2, 10, 5, 30), session(3, 10, 5, 0));

        assertEquals(at(10, 5, 7, 30), Reminders.next(sessions, at(10, 5, 7, 0)).orElseThrow());
        assertEquals(at(10, 5, 8, 0), Reminders.next(sessions, at(10, 5, 7, 30)).orElseThrow());
        assertEquals(at(10, 6, 7, 30), Reminders.next(sessions, at(10, 5, 8, 0)).orElseThrow());
    }

    @Test
    public void next_isEmptyWhenNothingIsToCome() {
        List<Session> sessions = List.of(session(1, 10, 5, 30), session(2, 10, 6, SessionDetails.NO_REMINDER));

        assertTrue(Reminders.next(sessions, at(10, 5, 7, 30)).isEmpty());
        assertTrue(Reminders.next(List.of(), at(10, 5, 7, 30)).isEmpty());
    }

    @Test
    public void next_findsAReminderOnTheDayBefore() {
        var early = new SessionDetails(details().type(), 10, 70, Mode.IN_PERSON, false, null, null, 0, null, 30);
        List<Session> sessions = List.of(new Session(1, 10, date(10, 5), early));

        assertEquals(at(10, 4, 23, 40), Reminders.next(sessions, at(10, 4, 20, 0)).orElseThrow());
    }
}