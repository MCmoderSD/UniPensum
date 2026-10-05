package de.mcmodersd.unipensum.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Recurrence rule of a series: every {@code intervalWeeks} weeks on {@code weekday},
 * from {@code first} to {@code last} (both inclusive).
 */
public record Schedule(DayOfWeek weekday, LocalDate first, LocalDate last, int intervalWeeks) {

    public Schedule {
        Objects.requireNonNull(weekday, "weekday");
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(last, "last");
        if (weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("The week only has Monday to Friday: " + weekday);
        }
        if (intervalWeeks < 1) {
            throw new IllegalArgumentException("intervalWeeks must be at least 1: " + intervalWeeks);
        }
    }

    public Schedule withFirst(LocalDate newFirst) {
        return new Schedule(weekday, newFirst, last, intervalWeeks);
    }

    public Schedule withLast(LocalDate newLast) {
        return new Schedule(weekday, first, newLast, intervalWeeks);
    }
}
