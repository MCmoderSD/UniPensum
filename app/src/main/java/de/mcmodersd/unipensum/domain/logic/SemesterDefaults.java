package de.mcmodersd.unipensum.domain.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Semester;

/**
 * Suggests the period for a new semester and for the events in it, so that most users only have to
 * confirm it. A lecture period lasts 16 weeks, about four months.
 */
public final class SemesterDefaults {

    public static final int LECTURE_WEEKS = 16;

    public record Period(LocalDate start, LocalDate end) {
    }

    private SemesterDefaults() { }

    /**
     * The last day of a lecture period that begins on {@code start}: 16 weeks later, less a day, so a
     * period that starts on a Monday ends on the Friday of its last week. The grid has no weekend, so an
     * end that would fall on one moves back to the Friday before.
     */
    public static LocalDate lectureEnd(LocalDate start) {
        var end = start.plusWeeks(LECTURE_WEEKS).minusDays(1);
        if (end.getDayOfWeek() == DayOfWeek.SATURDAY) return end.minusDays(1);
        if (end.getDayOfWeek() == DayOfWeek.SUNDAY) return end.minusDays(2);
        return end;
    }

    /**
     * The next 16-week term, starting on the first Monday of April or October, that has not ended
     * before {@code today} and does not overlap an existing semester.
     */
    public static Period suggest(LocalDate today, List<Semester> existing) {
        for (var year = today.getYear() - 1; year <= today.getYear() + 3; year++) {
            for (var month : new int[]{4, 10}) {
                var start = LocalDate.of(year, month, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
                var end = lectureEnd(start);
                if (end.isBefore(today) || overlapsAny(start, end, existing)) continue;
                return new Period(start, end);
            }
        }
        throw new IllegalStateException("No free semester slot within four years of " + today);
    }

    private static boolean overlapsAny(LocalDate start, LocalDate end, List<Semester> existing) {
        for (var semester : existing) {
            if (!start.isAfter(semester.end()) && !semester.start().isAfter(end)) return true;
        }
        return false;
    }
}