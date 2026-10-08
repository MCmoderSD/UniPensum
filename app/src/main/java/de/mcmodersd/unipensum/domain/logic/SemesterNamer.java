package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDate;

/** Derives the default semester name from its start date. Spelling is up to the UI layer. */
public final class SemesterNamer {

    public enum Season {
        WINTER,
        SUMMER
    }

    /**
     * @param year for {@link Season#SUMMER} the calendar year; for {@link Season#WINTER} the year
     *             the winter term begins in (so a start in January 2027 yields 2026, i.e. "26/27")
     */
    public record Label(Season season, int year) { }

    private SemesterNamer() { }

    public static Label label(LocalDate start) {
        var month = start.getMonthValue();
        if (month >= 3 && month <= 8) {
            return new Label(Season.SUMMER, start.getYear());
        }
        var winterYear = month >= 9 ? start.getYear() : start.getYear() - 1;
        return new Label(Season.WINTER, winterYear);
    }
}