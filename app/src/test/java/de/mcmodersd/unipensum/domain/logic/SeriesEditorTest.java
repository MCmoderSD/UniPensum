package de.mcmodersd.unipensum.domain.logic;

import static de.mcmodersd.unipensum.domain.Fixtures.SEMESTER;
import static de.mcmodersd.unipensum.domain.Fixtures.byId;
import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static de.mcmodersd.unipensum.domain.Fixtures.details;
import static de.mcmodersd.unipensum.domain.Fixtures.series;
import static de.mcmodersd.unipensum.domain.Fixtures.sessionsOf;
import static de.mcmodersd.unipensum.domain.Fixtures.weekly;
import static de.mcmodersd.unipensum.domain.Fixtures.withLecturer;
import static de.mcmodersd.unipensum.domain.Fixtures.withNote;
import static de.mcmodersd.unipensum.domain.Fixtures.withRoom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;

/**
 * Fixture: series 10, Mondays 2026-10-05 to 2026-11-02, so five sessions:
 * 101 = Oct 5, 102 = Oct 12, 103 = Oct 19, 104 = Oct 26, 105 = Nov 2.
 */
public class SeriesEditorTest {

    private final Series series = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 2)));
    private final List<Session> sessions = sessionsOf(series, 101);

    private ChangeSet edit(long sessionId, EditScope scope, SessionDetails details, Schedule schedule) {
        return SeriesEditor.edit(
                SEMESTER, series, sessions, sessionId, scope, details,
                byId(sessions, sessionId).day(), schedule
        );
    }

    // --- edit: this only ---

    @Test
    public void editThisOnly_changesJustThatSession() {
        var changed = withRoom(details(), "B2");

        var changes = edit(103, EditScope.THIS_ONLY, changed, series.schedule());

        assertEquals(1, changes.updatedSessions.size());
        assertEquals(new Session(103, 10, date(10, 19), changed), changes.updatedSessions.get(0));
        assertTrue(changes.updatedSeries.isEmpty());
        assertTrue(changes.newSeries.isEmpty());
        assertTrue(changes.deletedSessionIds.isEmpty());
    }

    @Test
    public void editThisOnly_canMoveToAnotherWeekday() {
        var changes = SeriesEditor.edit(
                SEMESTER, series, sessions, 103, EditScope.THIS_ONLY,
                details(), date(10, 21), series.schedule()
        );

        assertEquals(date(10, 21), changes.updatedSessions.get(0).day());
    }

    @Test
    public void editThisOnly_withoutAnyChange_isEmpty() {
        assertTrue(edit(103, EditScope.THIS_ONLY, details(), series.schedule()).isEmpty());
    }

    @Test
    public void editThisOnly_rejectsWeekendAndDatesOutsideTheSemester() {
        assertThrows(IllegalArgumentException.class, () -> SeriesEditor.edit(
                SEMESTER, series, sessions, 103,
                EditScope.THIS_ONLY, details(), date(10, 24), series.schedule()
        ));          // Saturday
        assertThrows(IllegalArgumentException.class, () -> SeriesEditor.edit(
                SEMESTER, series, sessions, 103,
                EditScope.THIS_ONLY, details(), date(10, 2), series.schedule()
        ));           // before the semester
    }

    // --- reminders ---

    private static SessionDetails withReminder(int minutes) {
        return details().withReminder(minutes);
    }

    @Test
    public void editThisOnly_changesTheReminderOfThatSessionOnly() {
        var changes = edit(103, EditScope.THIS_ONLY, withReminder(10), series.schedule());

        assertEquals(1, changes.updatedSessions.size());
        assertEquals(10, changes.updatedSessions.get(0).details().reminderMin());
        assertTrue(changes.updatedSeries.isEmpty());
    }

    @Test
    public void editAll_carriesTheReminderToEverySessionAndTheSeries() {
        var changes = edit(103, EditScope.ALL, withReminder(10), series.schedule());

        assertEquals(10, changes.updatedSeries.get(0).details().reminderMin());
        assertEquals(5, changes.updatedSessions.size());
        for (var session : changes.updatedSessions) assertEquals(10, session.details().reminderMin());
    }

    @Test
    public void editAll_keepsASessionThatHasAReminderOfItsOwn() {
        sessions.set(2, sessions.get(2).withDetails(withReminder(60)));

        var changes = edit(101, EditScope.ALL, withRoom(details(), "B2"), series.schedule());

        assertEquals(60, byId(changes.updatedSessions, 103).details().reminderMin());
        assertEquals("B2", byId(changes.updatedSessions, 103).details().room());
    }

    @Test
    public void editAll_switchingTheReminderOff_reachesEverySession() {
        var reminded = new Series(10, 1, withReminder(30), series.schedule());
        var remindedSessions = sessionsOf(reminded, 101);

        var changes = SeriesEditor.edit(
                SEMESTER, reminded, remindedSessions, 101, EditScope.ALL,
                withReminder(SessionDetails.NO_REMINDER), date(10, 5), reminded.schedule()
        );

        assertEquals(5, changes.updatedSessions.size());
        for (var session : changes.updatedSessions) assertFalse(session.details().hasReminder());
        assertFalse(changes.updatedSeries.get(0).details().hasReminder());
    }

    @Test
    public void editFollowing_carriesTheReminderFromThatSessionOn() {
        var changes = edit(103, EditScope.THIS_AND_FOLLOWING, withReminder(15), series.schedule());

        var created = changes.newSeries.get(0);
        assertEquals(15, created.series().details().reminderMin());
        for (var session : created.adopted()) assertEquals(15, session.details().reminderMin());
        assertEquals(3, created.adopted().size());
    }

    // --- edit: whole series ---

    @Test
    public void editAll_carriesChangedFieldToTemplateAndEverySession() {
        var changed = withRoom(details(), "B2");

        var changes = edit(103, EditScope.ALL, changed, series.schedule());

        assertEquals(changed, changes.updatedSeries.get(0).details());
        assertEquals(5, changes.updatedSessions.size());
        for (var session : changes.updatedSessions) assertEquals("B2", session.details().room());
        assertTrue(changes.newSessions.isEmpty());
        assertTrue(changes.deletedSessionIds.isEmpty());
    }

    @Test
    public void editAll_keepsIndividualDeviationsInOtherFields() {
        sessions.set(2, sessions.get(2).withDetails(withNote(details(), "bring laptop")));

        var changes = edit(101, EditScope.ALL, withRoom(details(), "B2"), series.schedule());

        var third = byId(changes.updatedSessions, 103);
        assertEquals("B2", third.details().room());
        assertEquals("bring laptop", third.details().note());
    }

    @Test
    public void editAll_changedLecturer_reachesEverySessionAndKeepsOtherDeviations() {
        sessions.set(2, sessions.get(2).withDetails(withNote(details(), "bring laptop")));

        var changes = edit(101, EditScope.ALL, withLecturer(details(), 9), series.schedule());

        assertEquals(9, changes.updatedSeries.get(0).details().lecturerId());
        assertEquals(5, changes.updatedSessions.size());
        for (var session : changes.updatedSessions) assertEquals(9, session.details().lecturerId());
        assertEquals("bring laptop", byId(changes.updatedSessions, 103).details().note());
    }

    @Test
    public void editAll_otherField_keepsALecturerSetOnOneSession() {
        sessions.set(2, sessions.get(2).withDetails(withLecturer(details(), 9)));

        var changes = edit(101, EditScope.ALL, withRoom(details(), "B2"), series.schedule());

        var third = byId(changes.updatedSessions, 103);
        assertEquals("B2", third.details().room());
        assertEquals(9, third.details().lecturerId());
    }

    @Test
    public void editThisOnly_removingTheLecturer_touchesOnlyThatSession() {
        var changes = edit(
                103, EditScope.THIS_ONLY,
                withLecturer(details(), SessionDetails.NO_LECTURER), series.schedule()
        );

        assertEquals(1, changes.updatedSessions.size());
        assertEquals(SessionDetails.NO_LECTURER, changes.updatedSessions.get(0).details().lecturerId());
        assertTrue(changes.updatedSeries.isEmpty());
    }

    @Test
    public void editAll_changedSchedule_regeneratesSessions() {
        var biweekly = new Schedule(DayOfWeek.MONDAY, date(10, 5), date(11, 2), 2);

        var changes = edit(103, EditScope.ALL, details(), biweekly);

        assertEquals(biweekly, changes.updatedSeries.get(0).schedule());
        assertEquals(List.of(101L, 102L, 103L, 104L, 105L), changes.deletedSessionIds);
        assertEquals(3, changes.newSessions.size());
        assertEquals(date(10, 5), changes.newSessions.get(0).day());
        assertEquals(date(10, 19), changes.newSessions.get(1).day());
        assertEquals(date(11, 2), changes.newSessions.get(2).day());
        assertEquals(10, changes.newSessions.get(0).seriesId());
        assertTrue(changes.updatedSessions.isEmpty());
    }

    @Test
    public void editAll_scheduleOutsideTheSemester_isRejected() {
        var tooEarly = weekly(DayOfWeek.MONDAY, date(9, 28), date(11, 2));
        assertThrows(IllegalArgumentException.class, () -> edit(103, EditScope.ALL, details(), tooEarly));
    }

    // --- edit: series template (course editor) ---

    @Test
    public void editSeries_carriesChangesToEverySessionAndKeepsOtherDeviations() {
        sessions.set(2, sessions.get(2).withDetails(withNote(details(), "bring laptop")));

        var changes = SeriesEditor.editSeries(
                SEMESTER, series, sessions,
                withRoom(details(), "B2"), series.schedule()
        );

        assertEquals("B2", changes.updatedSeries.get(0).details().room());
        assertEquals(5, changes.updatedSessions.size());
        assertEquals("B2", byId(changes.updatedSessions, 103).details().room());
        assertEquals("bring laptop", byId(changes.updatedSessions, 103).details().note());
    }

    @Test
    public void editSeries_unchanged_isEmpty() {
        assertTrue(SeriesEditor.editSeries(SEMESTER, series, sessions, series.details(), series.schedule()).isEmpty());
    }

    @Test
    public void editSeries_changedSchedule_regeneratesSessions() {
        var tuesdays = new Schedule(DayOfWeek.TUESDAY, date(10, 5), date(11, 2), 1);

        var changes = SeriesEditor.editSeries(SEMESTER, series, sessions, series.details(), tuesdays);

        assertEquals(List.of(101L, 102L, 103L, 104L, 105L), changes.deletedSessionIds);
        assertEquals(
                List.of(date(10, 6), date(10, 13), date(10, 20), date(10, 27)),
                changes.newSessions.stream().map(Session::day).toList()
        );
    }

    // --- edit: this and following ---

    @Test
    public void editFollowing_splitsTheSeriesAtTheSession() {
        var changed = withRoom(details(), "B2");

        var changes = edit(103, EditScope.THIS_AND_FOLLOWING, changed, series.schedule());

        // The old series stops the day before.
        assertEquals(1, changes.updatedSeries.size());
        assertEquals(10, changes.updatedSeries.get(0).id());
        assertEquals(date(10, 18), changes.updatedSeries.get(0).schedule().last());
        assertEquals(date(10, 5), changes.updatedSeries.get(0).schedule().first());
        assertEquals(details(), changes.updatedSeries.get(0).details());

        // The new series starts at the session and carries the change.
        assertEquals(1, changes.newSeries.size());
        var created = changes.newSeries.get(0);
        assertEquals(0, created.series().id());
        assertEquals(series.courseId(), created.series().courseId());
        assertEquals(date(10, 19), created.series().schedule().first());
        assertEquals(date(11, 2), created.series().schedule().last());
        assertEquals(changed, created.series().details());

        // Existing sessions move over instead of being recreated.
        assertTrue(created.sessions().isEmpty());
        assertEquals(List.of(103L, 104L, 105L), created.adopted().stream().map(Session::id).toList());
        for (var session : created.adopted()) assertEquals("B2", session.details().room());
        assertTrue(changes.deletedSessionIds.isEmpty());
    }

    @Test
    public void editFollowing_onTheFirstSession_behavesLikeEditingTheWholeSeries() {
        var changes = edit(101, EditScope.THIS_AND_FOLLOWING, withRoom(details(), "B2"), series.schedule());

        assertTrue(changes.newSeries.isEmpty());
        assertEquals(5, changes.updatedSessions.size());
        assertEquals(date(11, 2), changes.updatedSeries.get(0).schedule().last());
    }

    @Test
    public void editFollowing_withChangedRhythm_regeneratesTheFollowingSessions() {
        var biweekly = new Schedule(DayOfWeek.MONDAY, date(10, 5), date(11, 2), 2);

        var changes = edit(103, EditScope.THIS_AND_FOLLOWING, details(), biweekly);

        assertEquals(date(10, 18), changes.updatedSeries.get(0).schedule().last());
        assertEquals(List.of(103L, 104L, 105L), changes.deletedSessionIds);
        var created = changes.newSeries.get(0);
        assertTrue(created.adopted().isEmpty());
        assertEquals(2, created.series().schedule().intervalWeeks());
        assertEquals(date(10, 19), created.series().schedule().first());
        assertEquals(List.of(date(10, 19), date(11, 2)), created.sessions().stream().map(Session::day).toList());
    }

    @Test
    public void editFollowing_afterAnIndividualMove_keepsThePhaseOfTheChain() {
        var biweekly = series(20, new Schedule(DayOfWeek.THURSDAY, date(10, 5), date(11, 19), 2));
        var chain = new ArrayList<Session>(sessionsOf(biweekly, 201));       // Oct 8, 22, Nov 5, 19
        chain.set(2, chain.get(2).withDay(date(11, 6)));                         // Nov 5 moved to Friday

        var changes = SeriesEditor.edit(
                SEMESTER, biweekly, chain, 203, EditScope.THIS_AND_FOLLOWING,
                withRoom(details(), "B2"), date(11, 6), biweekly.schedule()
        );

        var created = changes.newSeries.get(0);
        // The moved session is adopted as it is, the pattern continues on the original Thursdays.
        assertEquals(List.of(203L, 204L), created.adopted().stream().map(Session::id).toList());
        assertEquals(date(11, 19), created.series().schedule().first());
        assertEquals(date(11, 5), changes.updatedSeries.get(0).schedule().last());
    }

    // --- delete ---

    @Test
    public void deleteThisOnly_removesOneSession() {
        var changes = SeriesEditor.delete(series, sessions, 103, EditScope.THIS_ONLY);

        assertEquals(List.of(103L), changes.deletedSessionIds);
        assertTrue(changes.deletedSeriesIds.isEmpty());
        assertTrue(changes.updatedSeries.isEmpty());
    }

    @Test
    public void deleteAll_removesTheSeries() {
        var changes = SeriesEditor.delete(series, sessions, 103, EditScope.ALL);

        assertEquals(List.of(10L), changes.deletedSeriesIds);
        assertTrue(changes.deletedSessionIds.isEmpty());
    }

    @Test
    public void deleteFollowing_removesLaterSessionsAndEndsTheSeriesTheDayBefore() {
        var changes = SeriesEditor.delete(series, sessions, 103, EditScope.THIS_AND_FOLLOWING);

        assertEquals(List.of(103L, 104L, 105L), changes.deletedSessionIds);
        assertEquals(date(10, 18), changes.updatedSeries.get(0).schedule().last());
        assertTrue(changes.deletedSeriesIds.isEmpty());
    }

    @Test
    public void deleteFollowing_onTheFirstSession_removesTheWholeSeries() {
        var changes = SeriesEditor.delete(series, sessions, 101, EditScope.THIS_AND_FOLLOWING);

        assertEquals(List.of(10L), changes.deletedSeriesIds);
        assertTrue(changes.deletedSessionIds.isEmpty());
        assertTrue(changes.updatedSeries.isEmpty());
    }

    @Test
    public void deleteFollowing_goesByDateNotById() {
        // Session 101 was moved behind session 103 and therefore counts as following it.
        sessions.set(0, sessions.get(0).withDay(date(10, 21)));

        var changes = SeriesEditor.delete(series, sessions, 103, EditScope.THIS_AND_FOLLOWING);

        assertEquals(List.of(103L, 101L, 104L, 105L), changes.deletedSessionIds);
    }

    @Test
    public void unknownSession_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SeriesEditor.delete(series, sessions, 999, EditScope.THIS_ONLY)
        );
        assertThrows(IllegalArgumentException.class, () -> SeriesEditor.edit(
                SEMESTER, series, sessions, 999,
                EditScope.ALL, details(), date(10, 5), series.schedule()
        ));
    }
}
