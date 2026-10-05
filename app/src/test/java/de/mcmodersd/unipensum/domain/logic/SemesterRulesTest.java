package de.mcmodersd.unipensum.domain.logic;

import static de.mcmodersd.unipensum.domain.Fixtures.SEMESTER;
import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static de.mcmodersd.unipensum.domain.Fixtures.series;
import static de.mcmodersd.unipensum.domain.Fixtures.sessionsOf;
import static de.mcmodersd.unipensum.domain.Fixtures.weekly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.logic.SemesterRules.ScheduleCheck;
import de.mcmodersd.unipensum.domain.logic.SemesterRules.SemesterCheck;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;

public class SemesterRulesTest {

    private static Semester semester(long id, LocalDate start, LocalDate end) {
        return new Semester(id, start, end, null);
    }

    // --- check ---

    @Test
    public void check_validSemester() {
        assertEquals(SemesterCheck.OK, SemesterRules.check(SEMESTER, List.of()));
    }

    @Test
    public void check_endNotAfterStart_isInvalid() {
        assertEquals(SemesterCheck.INVALID_RANGE,
                SemesterRules.check(semester(2, date(10, 5), date(10, 5)), List.of()));
        assertEquals(SemesterCheck.INVALID_RANGE,
                SemesterRules.check(semester(2, date(10, 6), date(10, 5)), List.of()));
    }

    @Test
    public void check_overlap_isRejected() {
        Semester other = semester(2, date(2, 12), date(3, 31));
        // Shares exactly one day with SEMESTER (2027-02-12).
        assertEquals(SemesterCheck.OVERLAPS_OTHER, SemesterRules.check(SEMESTER, List.of(other)));
    }

    @Test
    public void check_adjacentSemesters_areFine() {
        Semester other = semester(2, date(2, 13), date(3, 31));
        assertEquals(SemesterCheck.OK, SemesterRules.check(SEMESTER, List.of(other)));
    }

    @Test
    public void check_ignoresItself() {
        assertEquals(SemesterCheck.OK, SemesterRules.check(SEMESTER, List.of(SEMESTER)));
    }

    // --- move target and schedule ---

    @Test
    public void moveTarget_onlyWeekdaysInsideSemester() {
        assertTrue(SemesterRules.isValidMoveTarget(SEMESTER, date(10, 9)));      // Friday
        assertFalse(SemesterRules.isValidMoveTarget(SEMESTER, date(10, 10)));    // Saturday
        assertFalse(SemesterRules.isValidMoveTarget(SEMESTER, date(10, 11)));    // Sunday
        assertFalse(SemesterRules.isValidMoveTarget(SEMESTER, date(10, 2)));     // before start
        assertFalse(SemesterRules.isValidMoveTarget(SEMESTER, date(2, 15)));     // after end
    }

    @Test
    public void checkSchedule_distinguishesCases() {
        assertEquals(ScheduleCheck.OK,
                SemesterRules.checkSchedule(SEMESTER, weekly(DayOfWeek.MONDAY, date(10, 5), date(2, 12))));
        assertEquals(ScheduleCheck.EMPTY_RANGE,
                SemesterRules.checkSchedule(SEMESTER, weekly(DayOfWeek.MONDAY, date(11, 2), date(10, 5))));
        assertEquals(ScheduleCheck.OUTSIDE_SEMESTER,
                SemesterRules.checkSchedule(SEMESTER, weekly(DayOfWeek.MONDAY, date(9, 28), date(11, 2))));
        assertEquals(ScheduleCheck.OUTSIDE_SEMESTER,
                SemesterRules.checkSchedule(SEMESTER, weekly(DayOfWeek.MONDAY, date(10, 5), date(2, 15))));
    }

    // --- resize ---

    private static final Semester OLD = semester(1, date(10, 5), date(11, 13));      // Mon to Fri

    private static List<Session> allSessions(Series... series) {
        List<Session> result = new ArrayList<>();
        long next = 100;
        for (Series s : series) {
            List<Session> sessions = sessionsOf(s, next);
            result.addAll(sessions);
            next += 100;
        }
        return result;
    }

    @Test
    public void resize_shorteningTheEnd_clipsSeriesAndDeletesLaterSessions() {
        Semester updated = semester(1, date(10, 5), date(10, 30));                    // ends Friday
        Series series = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 13)));
        List<Session> sessions = allSessions(series);                                 // Oct 5 ... Nov 9

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(series), sessions);

        assertEquals(1, changes.updatedSeries.size());
        assertEquals(date(10, 30), changes.updatedSeries.get(0).schedule().last());
        // Mondays are 100 = Oct 5 to 105 = Nov 9: Nov 2 and Nov 9 are gone, Oct 5 to Oct 26 stay.
        assertEquals(List.of(104L, 105L), changes.deletedSessionIds);
        assertTrue(changes.newSessions.isEmpty());
    }

    @Test
    public void resize_extendingTheEnd_prolongsSeriesThatRanToTheOldEnd() {
        Semester updated = semester(1, date(10, 5), date(11, 27));
        Series running = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 13)));
        Series ended = series(20, weekly(DayOfWeek.TUESDAY, date(10, 5), date(10, 27)));
        List<Session> sessions = allSessions(running, ended);

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(running, ended), sessions);

        assertEquals(1, changes.updatedSeries.size());
        assertEquals(10, changes.updatedSeries.get(0).id());
        assertEquals(date(11, 27), changes.updatedSeries.get(0).schedule().last());
        assertEquals(2, changes.newSessions.size());
        assertEquals(date(11, 16), changes.newSessions.get(0).day());
        assertEquals(date(11, 23), changes.newSessions.get(1).day());
        assertEquals(10, changes.newSessions.get(0).seriesId());
        assertTrue(changes.deletedSessionIds.isEmpty());
    }

    @Test
    public void resize_extendingTheEnd_keepsPhaseOfBiweeklySeries() {
        Semester updated = semester(1, date(10, 5), date(11, 27));
        Series biweekly = series(10, new Schedule(DayOfWeek.THURSDAY, date(10, 5), date(11, 13), 2));
        List<Session> sessions = allSessions(biweekly);                               // Oct 8, 22, Nov 5

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(biweekly), sessions);

        assertEquals(1, changes.newSessions.size());
        assertEquals(date(11, 19), changes.newSessions.get(0).day());
    }

    @Test
    public void resize_shorteningTheStart_movesFirstDayAndDeletesEarlierSessions() {
        Semester updated = semester(1, date(10, 19), date(11, 13));
        Series weekly = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 13)));
        Series biweekly = series(20, new Schedule(DayOfWeek.THURSDAY, date(10, 5), date(11, 13), 2));
        List<Session> sessions = allSessions(weekly, biweekly);

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(weekly, biweekly), sessions);

        assertEquals(date(10, 19), changes.updatedSeries.get(0).schedule().first());
        // Biweekly chain Oct 8, 22, Nov 5: the first one on or after Oct 19 is Oct 22.
        assertEquals(date(10, 22), changes.updatedSeries.get(1).schedule().first());
        // Weekly Oct 5 (100) and Oct 12 (101) plus biweekly Oct 8 (200).
        assertEquals(List.of(100L, 101L, 200L), changes.deletedSessionIds);
    }

    @Test
    public void resize_seriesLeftWithoutAnyOccurrence_isDeleted() {
        Semester updated = semester(1, date(10, 19), date(11, 13));
        Series early = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(10, 12)));
        List<Session> sessions = allSessions(early);

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(early), sessions);

        assertEquals(List.of(10L), changes.deletedSeriesIds);
        // Its sessions go with the series, they are not listed one by one.
        assertTrue(changes.deletedSessionIds.isEmpty());
        assertTrue(changes.updatedSeries.isEmpty());
    }

    @Test
    public void resize_extendingTheStart_leavesSeriesUntouched() {
        Semester updated = semester(1, date(9, 28), date(11, 13));
        Series series = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 13)));

        ChangeSet changes = SemesterRules.resize(OLD, updated, List.of(series), allSessions(series));

        assertTrue(changes.isEmpty());
    }

    @Test
    public void resize_unchangedPeriod_changesNothing() {
        Series series = series(10, weekly(DayOfWeek.MONDAY, date(10, 5), date(11, 13)));

        ChangeSet changes = SemesterRules.resize(OLD, OLD, List.of(series), allSessions(series));

        assertTrue(changes.isEmpty());
    }
}
