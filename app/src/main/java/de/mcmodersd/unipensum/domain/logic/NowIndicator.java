package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Where the "now" marker of the week grid belongs: in which of the five day columns and at which minute of
 * the day. There is none if today is not a day of the shown week (another week, or the weekend, which the
 * grid has no column for) or if the time lies outside the visible hours.
 */
public final class NowIndicator {

    private NowIndicator() {
    }

    /**
     * @param dayIndex 0 for Monday to 4 for Friday
     * @param minutes  minutes since midnight
     */
    public record Position(int dayIndex, int minutes) {
    }

    /**
     * @param monday    the Monday of the shown week
     * @param startHour first visible hour
     * @param endHour   end of the last visible hour, exclusive as a row but the bottom line of the grid
     *                  still counts as visible
     */
    public static Optional<Position> at(LocalDate monday, LocalDateTime now, int startHour, int endHour) {
        long dayIndex = ChronoUnit.DAYS.between(monday, now.toLocalDate());
        if (dayIndex < 0 || dayIndex > 4) return Optional.empty();
        int minutes = now.getHour() * 60 + now.getMinute();
        if (minutes < startHour * 60 || minutes > endHour * 60) return Optional.empty();
        return Optional.of(new Position((int) dayIndex, minutes));
    }
}
