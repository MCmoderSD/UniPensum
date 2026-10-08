package de.mcmodersd.unipensum.domain.model;

import java.util.Objects;

/** A recurring event of a course, the template its {@link Session}s are generated from. */
public record Series(long id, long courseId, SessionDetails details, Schedule schedule) {

    public Series {
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(schedule, "schedule");
    }

    public Series withDetails(SessionDetails newDetails) {
        return new Series(id, courseId, newDetails, schedule);
    }

    public Series withSchedule(Schedule newSchedule) {
        return new Series(id, courseId, details, newSchedule);
    }
}