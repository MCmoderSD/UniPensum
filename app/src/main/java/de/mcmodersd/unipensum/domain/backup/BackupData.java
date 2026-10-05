package de.mcmodersd.unipensum.domain.backup;

import java.util.List;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;

public record BackupData(
        List<Lecturer> lecturers,
        List<Semester> semesters,
        List<Course> courses,
        List<Series> series,
        List<Session> sessions
) {
    public BackupData {
        lecturers = List.copyOf(lecturers);
        semesters = List.copyOf(semesters);
        courses = List.copyOf(courses);
        series = List.copyOf(series);
        sessions = List.copyOf(sessions);
    }
}