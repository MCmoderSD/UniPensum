package de.mcmodersd.unipensum.data;

import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Session;

/**
 * A session together with what the grid needs to draw it.
 *
 * @param lecturer the session's lecturer, {@code null} if it has none
 */
public record SessionView(Session session, long courseId, long semesterId, String courseName, CourseColor color,
                          Lecturer lecturer) {
}
