package de.mcmodersd.unipensum.domain.backup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import de.mcmodersd.unipensum.domain.logic.SemesterRules;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;

public final class BackupCleaner {

    public record Result(BackupData data, BackupReport report) { }

    private int skipped;
    private int adjusted;

    private BackupCleaner() { }

    public static Result clean(BackupData raw, int unreadable, int adjustedWhileReading) {
        var cleaner = new BackupCleaner();
        cleaner.skipped = unreadable;
        cleaner.adjusted = adjustedWhileReading;
        return cleaner.run(raw);
    }

    private Result run(BackupData raw) {
        var lecturers = cleanLecturers(raw.lecturers());
        var semesters = cleanSemesters(raw.semesters());
        var courses = cleanCourses(raw.courses(), semesters);
        var series = cleanSeries(raw.series(), courses, semesters, lecturers.keySet());
        var sessions = cleanSessions(raw.sessions(), series, courses, semesters, lecturers.keySet());

        var data = new BackupData(
                new ArrayList<>(lecturers.values()), new ArrayList<>(semesters.values()),
                new ArrayList<>(courses.values()), new ArrayList<>(series.values()), sessions
        );
        return new Result(
                data, new BackupReport(
                        data.lecturers().size(), data.semesters().size(),
                        data.courses().size(), data.series().size(), data.sessions().size(), skipped, adjusted
                )
        );
    }

    private LinkedHashMap<Long, Lecturer> cleanLecturers(List<Lecturer> raw) {
        var kept = new LinkedHashMap<Long, Lecturer>();
        for (var lecturer : raw) {
            var clean = lecturer.normalized();
            if (lecturer.id() <= 0 || kept.containsKey(lecturer.id()) || clean.lastName().isEmpty()) {
                skipped++;
                continue;
            }
            kept.put(lecturer.id(), clean);
        }
        return kept;
    }

    private LinkedHashMap<Long, Semester> cleanSemesters(List<Semester> raw) {
        var seen = new HashSet<Long>();
        var candidates = new ArrayList<Semester>();
        for (var semester : raw) {
            if (semester.id() <= 0 || !seen.add(semester.id())) {
                skipped++;
                continue;
            }
            candidates.add(
                    new Semester(
                            semester.id(), semester.start(), semester.end(),
                            TextSanitizer.lineOrNull(semester.customName(), TextSanitizer.MAX_NAME)
                    )
            );
        }

        // Earlier semesters win when two overlap.
        candidates.sort(Comparator.comparing(Semester::start).thenComparingLong(Semester::id));

        var kept = new ArrayList<Semester>();
        for (var candidate : candidates) {
            if (SemesterRules.check(candidate, kept) == SemesterRules.SemesterCheck.OK) kept.add(candidate);
            else skipped++;
        }
        var byId = new LinkedHashMap<Long, Semester>();
        for (var semester : kept) byId.put(semester.id(), semester);
        return byId;
    }

    private LinkedHashMap<Long, Course> cleanCourses(List<Course> raw, LinkedHashMap<Long, Semester> semesters) {
        var kept = new LinkedHashMap<Long, Course>();
        for (var course : raw) {
            var name = TextSanitizer.line(course.name(), TextSanitizer.MAX_NAME);
            if (course.id() <= 0 || kept.containsKey(course.id()) || name.isEmpty()
                    || !semesters.containsKey(course.semesterId())) {
                skipped++;
                continue;
            }
            kept.put(
                    course.id(), new Course(
                            course.id(), course.semesterId(), name, course.color(),
                            webLinkOrNull(course.moodleLink())
                    )
            );
        }
        return kept;
    }

    private LinkedHashMap<Long, Series> cleanSeries(List<Series> raw, LinkedHashMap<Long, Course> courses, LinkedHashMap<Long, Semester> semesters, Set<Long> lecturerIds) {
        var kept = new LinkedHashMap<Long, Series>();
        for (var series : raw) {
            var course = courses.get(series.courseId());
            var fits = series.id() > 0 && !kept.containsKey(series.id()) && course != null
                    && SemesterRules.checkSchedule(semesters.get(course.semesterId()), series.schedule())
                    == SemesterRules.ScheduleCheck.OK;
            // The details are cleaned last, so that only entries that are kept count as adjusted.
            var details = fits ? cleanDetails(series.details(), lecturerIds) : null;
            if (details == null) {
                skipped++;
                continue;
            }
            kept.put(series.id(), new Series(series.id(), series.courseId(), details, series.schedule()));
        }
        return kept;
    }

    private ArrayList<Session> cleanSessions(List<Session> raw, LinkedHashMap<Long, Series> series, LinkedHashMap<Long, Course> courses,
                                             LinkedHashMap<Long, Semester> semesters, Set<Long> lecturerIds) {
        var seen = new HashSet<Long>();
        var kept = new ArrayList<Session>();
        for (var session : raw) {
            var parent = series.get(session.seriesId());
            var fits = session.id() > 0 && seen.add(session.id()) && parent != null
                    && SemesterRules.isValidMoveTarget(
                            semesters.get(courses.get(parent.courseId()).semesterId()), session.day()
                    );
            var details = fits ? cleanDetails(session.details(), lecturerIds) : null;
            if (details == null) {
                skipped++;
                continue;
            }
            kept.add(new Session(session.id(), session.seriesId(), session.day(), details));
        }
        return kept;
    }

    /** @return the cleaned details, {@code null} if the times make no sense */
    private SessionDetails cleanDetails(SessionDetails details, Set<Long> lecturerIds) {
        if (!details.hasValidTimes()) return null;
        var lecturer = details.lecturerId();
        if (lecturer != SessionDetails.NO_LECTURER && !lecturerIds.contains(lecturer)) {
            lecturer = SessionDetails.NO_LECTURER;
            adjusted++;
        }
        var candidate = details.withLecturer(lecturer);
        SessionDetails clean;
        try {
            clean = candidate.normalized();
        } catch (IllegalArgumentException notAWebLink) {
            clean = candidate.withLink(null).normalized();
            adjusted++;
        }
        return clean;
    }

    private String webLinkOrNull(String link) {
        try {
            return TextSanitizer.webLink(link);
        } catch (IllegalArgumentException notAWebLink) {
            adjusted++;
            return null;
        }
    }
}