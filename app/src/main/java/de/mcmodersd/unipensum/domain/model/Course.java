package de.mcmodersd.unipensum.domain.model;

import java.util.Objects;

/** @param moodleLink the course page in Moodle, {@code null} if there is none */
public record Course(long id, long semesterId, String name, CourseColor color, String moodleLink) {

    public Course {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
    }
}
