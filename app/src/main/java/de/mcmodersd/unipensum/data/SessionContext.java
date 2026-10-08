package de.mcmodersd.unipensum.data;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;

/**
 * Everything above a session in the hierarchy, as needed to show or edit it.
 *
 * @param lecturer the session's lecturer, {@code null} if it has none
 */
public record SessionContext(Session session, Series series, Course course, Semester semester, Lecturer lecturer) {
}