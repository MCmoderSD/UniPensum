package de.mcmodersd.unipensum.data;

import java.util.List;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Series;

public record CourseWithSeries(Course course, List<Series> series) {
}