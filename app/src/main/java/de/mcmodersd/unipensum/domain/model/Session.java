package de.mcmodersd.unipensum.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** A single occurrence on a concrete date. Fully materialized, so every field is editable on its own. */
public record Session(long id, long seriesId, LocalDate day, SessionDetails details) {

    public Session {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(details, "details");
    }

    public Session withDay(LocalDate newDay) {
        return new Session(id, seriesId, newDay, details);
    }

    public Session withDetails(SessionDetails newDetails) {
        return new Session(id, seriesId, day, newDetails);
    }
}
