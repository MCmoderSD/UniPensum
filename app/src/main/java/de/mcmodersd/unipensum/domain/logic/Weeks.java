package de.mcmodersd.unipensum.domain.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;

/** Maps pager positions to weeks: position 0 is the week of {@link #ANCHOR}, one position per week. */
public final class Weeks {

    /** A Monday, early enough for any semester anyone will enter. */
    public static final LocalDate ANCHOR = LocalDate.of(2020, 1, 6);

    /** Roughly 30 years, until the end of 2049. */
    public static final int PAGE_COUNT = 52 * 30;

    private Weeks() { }

    public static LocalDate mondayOf(int position) {
        return ANCHOR.plusWeeks(position);
    }

    /** Position of the week containing {@code day}; negative before the anchor. */
    public static int positionOf(LocalDate day) {
        return (int) Math.floorDiv(ChronoUnit.DAYS.between(ANCHOR, day), 7L);
    }

    /** The current week, or the coming one on Saturday and Sunday, which the app does not show. */
    public static int initialPosition(LocalDate today) {
        var weekend = today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;
        var position = positionOf(today) + (weekend ? 1 : 0);
        return Math.max(0, Math.min(PAGE_COUNT - 1, position));
    }

    public static int isoWeekNumber(LocalDate monday) {
        return monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
