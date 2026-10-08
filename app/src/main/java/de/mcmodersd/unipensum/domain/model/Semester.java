package de.mcmodersd.unipensum.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @param customName {@code null} means the name is derived from the start date
 */
public record Semester(long id, LocalDate start, LocalDate end, String customName) {

    public Semester {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    /** Both boundaries are inclusive. */
    public boolean contains(LocalDate day) {
        return !day.isBefore(start) && !day.isAfter(end);
    }
}