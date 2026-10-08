package de.mcmodersd.unipensum.data;

import java.util.List;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;

/** A course with its semester and series, as the course editor needs to start from. */
public record CourseContext(Course course, Semester semester, List<Series> series) {
}