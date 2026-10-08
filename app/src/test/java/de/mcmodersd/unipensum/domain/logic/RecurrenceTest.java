package de.mcmodersd.unipensum.domain.logic;

import static de.mcmodersd.unipensum.domain.Fixtures.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import de.mcmodersd.unipensum.domain.model.Schedule;

public class RecurrenceTest {

    @Test
    public void weekly_listsEveryWeekIncludingLastDay() {
        var schedule = new Schedule(DayOfWeek.MONDAY, date(10, 5), date(11, 2), 1);
        assertEquals(
                List.of(date(10, 5), date(10, 12), date(10, 19), date(10, 26), date(11, 2)),
                Recurrence.occurrences(schedule)
        );
    }

    @Test
    public void firstDayNotOnWeekday_startsWithNextMatchingWeekday() {
        var schedule = new Schedule(DayOfWeek.MONDAY, date(10, 6), date(10, 26), 1);
        assertEquals(List.of(date(10, 12), date(10, 19), date(10, 26)), Recurrence.occurrences(schedule));
    }

    @Test
    public void biweekly_skipsEveryOtherWeek() {
        var schedule = new Schedule(DayOfWeek.THURSDAY, date(10, 5), date(11, 30), 2);
        assertEquals(
                List.of(date(10, 8), date(10, 22), date(11, 5), date(11, 19)),
                Recurrence.occurrences(schedule)
        );
    }

    @Test
    public void freeInterval_everyThreeWeeks() {
        var schedule = new Schedule(DayOfWeek.FRIDAY, date(10, 5), date(11, 30), 3);
        assertEquals(List.of(date(10, 9), date(10, 30), date(11, 20)), Recurrence.occurrences(schedule));
    }

    @Test
    public void lastBeforeFirst_isEmpty() {
        var schedule = new Schedule(DayOfWeek.MONDAY, date(10, 12), date(10, 5), 1);
        assertTrue(Recurrence.occurrences(schedule).isEmpty());
    }

    @Test
    public void firstOnOrAfter_keepsPhaseOfBiweeklyChain() {
        var schedule = new Schedule(DayOfWeek.THURSDAY, date(10, 5), date(11, 30), 2);
        assertEquals(Optional.of(date(10, 22)), Recurrence.firstOnOrAfter(schedule, date(10, 20)));
        assertEquals(Optional.of(date(10, 22)), Recurrence.firstOnOrAfter(schedule, date(10, 22)));
    }

    @Test
    public void firstOnOrAfter_pastTheEnd_isEmpty() {
        var schedule = new Schedule(DayOfWeek.MONDAY, date(10, 5), date(10, 12), 1);
        assertEquals(Optional.empty(), Recurrence.firstOnOrAfter(schedule, LocalDate.of(2026, 10, 13)));
    }
}