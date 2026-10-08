package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mcmodersd.unipensum.domain.model.Schedule;

public final class Recurrence {

    private Recurrence() {
    }

    /**
     * All dates of the schedule in ascending order. The first one is the first matching weekday
     * on or after {@code schedule.first()}, then every {@code intervalWeeks} weeks until {@code last}.
     */
    public static List<LocalDate> occurrences(Schedule schedule) {
        var result = new ArrayList<LocalDate>();
        var day = schedule.first().with(TemporalAdjusters.nextOrSame(schedule.weekday()));
        while (!day.isAfter(schedule.last())) {
            result.add(day);
            day = day.plusWeeks(schedule.intervalWeeks());
        }
        return result;
    }

    /** First occurrence on or after {@code from}, keeping the phase of the existing chain. */
    public static Optional<LocalDate> firstOnOrAfter(Schedule schedule, LocalDate from) {
        for (var day : occurrences(schedule)) {
            if (!day.isBefore(from)) return Optional.of(day);
        }
        return Optional.empty();
    }
}
