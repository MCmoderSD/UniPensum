package de.mcmodersd.unipensum.domain.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import de.mcmodersd.unipensum.domain.logic.SemesterDefaults.Period;
import de.mcmodersd.unipensum.domain.model.Semester;

public class SemesterDefaultsTest {

    @Test
    public void withoutSemesters_suggestsTheComingWinterTerm() {
        var period = SemesterDefaults.suggest(LocalDate.of(2026, 10, 4), List.of());

        assertEquals(LocalDate.of(2026, 10, 5), period.start());
        assertEquals(DayOfWeek.MONDAY, period.start().getDayOfWeek());
        // 16 weeks, ending on the Friday of the last one.
        assertEquals(LocalDate.of(2027, 1, 22), period.end());
        assertEquals(DayOfWeek.FRIDAY, period.end().getDayOfWeek());
    }

    @Test
    public void afterTheTermHasEnded_suggestsTheNextSummerTerm() {
        var period = SemesterDefaults.suggest(LocalDate.of(2027, 1, 25), List.of());

        assertEquals(LocalDate.of(2027, 4, 5), period.start());
        assertEquals(LocalDate.of(2027, 7, 23), period.end());
    }

    @Test
    public void skipsTermsThatOverlapAnExistingSemester() {
        var winter = new Semester(1, LocalDate.of(2026, 10, 5), LocalDate.of(2027, 2, 12), null);

        var period = SemesterDefaults.suggest(LocalDate.of(2026, 10, 4), List.of(winter));

        assertEquals(LocalDate.of(2027, 4, 5), period.start());
    }

    @Test
    public void aTermInProgress_isStillSuggested() {
        var period = SemesterDefaults.suggest(LocalDate.of(2026, 11, 18), List.of());

        assertEquals(LocalDate.of(2026, 10, 5), period.start());
    }

    @Test
    public void lectureEnd_isSixteenWeeksAfterTheStart_endingOnAFridayForAMonday() {
        var start = LocalDate.of(2026, 10, 5);

        var end = SemesterDefaults.lectureEnd(start);

        assertEquals(DayOfWeek.FRIDAY, end.getDayOfWeek());
        assertEquals(15, ChronoUnit.WEEKS.between(start, end));       // 15 full weeks plus the days of the 16th
        assertEquals(LocalDate.of(2027, 1, 22), end);
    }

    @Test
    public void lectureEnd_forAMidweekStart_endsTheDayBeforeTheSameWeekdayInTheSixteenthWeek() {
        // Wednesday, 7 October 2026: 16 weeks later is Wednesday, 27 January 2027, so the period ends on Tuesday.
        assertEquals(LocalDate.of(2027, 1, 26), SemesterDefaults.lectureEnd(LocalDate.of(2026, 10, 7)));
    }

    @Test
    public void lectureEnd_neverFallsOnAWeekend() {
        var start = LocalDate.of(2026, 10, 1);
        for (var i = 0; i < 400; i++) {
            var end = SemesterDefaults.lectureEnd(start.plusDays(i));
            var day = end.getDayOfWeek();
            assertTrue("Weekend end for start " + start.plusDays(i), day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);
            // Never longer than 16 weeks, and the weekend move costs at most two days.
            var days = ChronoUnit.DAYS.between(start.plusDays(i), end) + 1;
            assertTrue(days <= 16 * 7 && days >= 16 * 7 - 2);
        }
    }

    @Test
    public void lectureEnd_forAWeekendStart_movesBackToFriday() {
        // Sunday, 4 October 2026 + 16 weeks - 1 day = Saturday, 23 January 2027 -> Friday, 22 January.
        assertEquals(LocalDate.of(2027, 1, 22), SemesterDefaults.lectureEnd(LocalDate.of(2026, 10, 4)));
    }
}