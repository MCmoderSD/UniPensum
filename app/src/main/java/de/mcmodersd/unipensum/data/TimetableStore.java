package de.mcmodersd.unipensum.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import de.mcmodersd.unipensum.data.db.CourseDao;
import de.mcmodersd.unipensum.data.db.LecturerDao;
import de.mcmodersd.unipensum.data.db.SemesterDao;
import de.mcmodersd.unipensum.data.db.SeriesDao;
import de.mcmodersd.unipensum.data.db.SessionDao;
import de.mcmodersd.unipensum.domain.backup.BackupData;
import de.mcmodersd.unipensum.domain.logic.ChangeSet;
import de.mcmodersd.unipensum.domain.logic.Recurrence;
import de.mcmodersd.unipensum.domain.logic.SemesterRules;
import de.mcmodersd.unipensum.domain.logic.SeriesEditor;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;

/**
 * Synchronous data operations against an open database. Every write runs in its own transaction
 * and throws {@link IllegalArgumentException} for input that violates the semester rules.
 * {@link TimetableRepository} wraps these for the UI; tests call them directly.
 */
public final class TimetableStore {

    private TimetableStore() {
    }

    // --- reads ---

    public static List<Semester> listSemesters(SQLiteDatabase db) {
        return SemesterDao.list(db);
    }

    public static List<Lecturer> listLecturers(SQLiteDatabase db) {
        return LecturerDao.list(db);
    }

    public static Timetable loadTimetable(SQLiteDatabase db) {
        var sql = "SELECT s.*, c.id AS course_id, c.semester_id AS semester_id, "
                + "c.name AS course_name, c.color AS course_color, "
                + "l.id AS lec_id, l.first_name AS lec_first_name, l.last_name AS lec_last_name, "
                + "l.email AS lec_email, l.phone AS lec_phone "
                + "FROM session s JOIN series r ON s.series_id = r.id JOIN course c ON r.course_id = c.id "
                + "LEFT JOIN lecturer l ON s.lecturer_id = l.id "
                + "ORDER BY s.day, s.start_min, s.id";
        var views = new ArrayList<SessionView>();
        try (var cursor = db.rawQuery(sql, null)) {
            var courseId = cursor.getColumnIndexOrThrow("course_id");
            var semesterId = cursor.getColumnIndexOrThrow("semester_id");
            var name = cursor.getColumnIndexOrThrow("course_name");
            var color = cursor.getColumnIndexOrThrow("course_color");
            while (cursor.moveToNext()) {
                views.add(new SessionView(
                        SessionDao.read(cursor),
                        cursor.getLong(courseId),
                        cursor.getLong(semesterId),
                        cursor.getString(name),
                        CourseColor.fromKey(cursor.getString(color)),
                        LecturerDao.read(cursor, "lec_")));
            }
        }
        return new Timetable(SemesterDao.list(db), views);
    }

    /**
     * The sessions that have a reminder and take place on {@code from} or later, with the name and the Moodle
     * link of their course, which is what a reminder shows. Earliest first.
     */
    public static List<ReminderView> loadReminders(SQLiteDatabase db, LocalDate from) {
        var sql = "SELECT s.*, c.name AS course_name, c.moodle_link AS moodle_link "
                + "FROM session s JOIN series r ON s.series_id = r.id JOIN course c ON r.course_id = c.id "
                + "WHERE s.day >= ? AND s.reminder_min IS NOT NULL ORDER BY s.day, s.start_min, s.id";
        var views = new ArrayList<ReminderView>();
        try (var cursor = db.rawQuery(sql, new String[]{String.valueOf(from.toEpochDay())})) {
            var name = cursor.getColumnIndexOrThrow("course_name");
            var moodle = cursor.getColumnIndexOrThrow("moodle_link");
            while (cursor.moveToNext()) {
                views.add(new ReminderView(SessionDao.read(cursor), cursor.getString(name),
                        cursor.isNull(moodle) ? null : cursor.getString(moodle)));
            }
        }
        return views;
    }

    public static List<CourseWithSeries> listCourses(SQLiteDatabase db, long semesterId) {
        var result = new ArrayList<CourseWithSeries>();
        for (var course : CourseDao.listBySemester(db, semesterId)) {
            result.add(new CourseWithSeries(course, SeriesDao.listByCourse(db, course.id())));
        }
        return result;
    }

    public static CourseContext loadCourse(SQLiteDatabase db, long courseId) {
        Course course = require(CourseDao.get(db, courseId), "course", courseId);
        Semester semester = require(SemesterDao.get(db, course.semesterId()), "semester", course.semesterId());
        return new CourseContext(course, semester, SeriesDao.listByCourse(db, courseId));
    }

    public static SessionContext loadSessionContext(SQLiteDatabase db, long sessionId) {
        Session session = require(SessionDao.get(db, sessionId), "session", sessionId);
        Series series = require(SeriesDao.get(db, session.seriesId()), "series", session.seriesId());
        Course course = require(CourseDao.get(db, series.courseId()), "course", series.courseId());
        Semester semester = require(SemesterDao.get(db, course.semesterId()), "semester", course.semesterId());
        var lecturerId = session.details().lecturerId();
        Lecturer lecturer = lecturerId == SessionDetails.NO_LECTURER ? null : LecturerDao.get(db, lecturerId);
        return new SessionContext(session, series, course, semester, lecturer);
    }

    // --- backup ---

    /** Everything in the database, for a backup. */
    public static BackupData exportData(SQLiteDatabase db) {
        var semesters = SemesterDao.list(db);
        var courses = new ArrayList<Course>();
        var series = new ArrayList<Series>();
        var sessions = new ArrayList<Session>();
        for (var semester : semesters) {
            courses.addAll(CourseDao.listBySemester(db, semester.id()));
            series.addAll(SeriesDao.listBySemester(db, semester.id()));
            sessions.addAll(SessionDao.listBySemester(db, semester.id()));
        }
        return new BackupData(LecturerDao.list(db), semesters, courses, series, sessions);
    }

    /**
     * Replaces everything in the database with {@code data}, in one transaction: if anything goes wrong the
     * old data is still there. The data must be consistent (see {@code BackupCleaner}); the ids in it only
     * tie its lists together and are replaced by new ones.
     *
     * @throws IllegalArgumentException if an entry names a parent that is not in the data
     */
    public static void replaceAll(SQLiteDatabase db, BackupData data) {
        transact(db, () -> {
            // Deleting the semesters takes courses, series and sessions with it; lecturers are separate.
            SemesterDao.deleteAll(db);
            LecturerDao.deleteAll(db);

            var lecturerIds = new HashMap<Long, Long>();
            for (var lecturer : data.lecturers()) {
                lecturerIds.put(lecturer.id(), LecturerDao.insert(db, lecturer));
            }
            var semesterIds = new HashMap<Long, Long>();
            for (var semester : data.semesters()) {
                semesterIds.put(semester.id(), SemesterDao.insert(db, semester));
            }
            var courseIds = new HashMap<Long, Long>();
            for (var course : data.courses()) {
                var semesterId = mapped(semesterIds, course.semesterId(), "semester");
                courseIds.put(course.id(), CourseDao.insert(db,
                        new Course(0, semesterId, course.name(), course.color(), course.moodleLink())));
            }
            var seriesIds = new HashMap<Long, Long>();
            for (var series : data.series()) {
                var courseId = mapped(courseIds, series.courseId(), "course");
                seriesIds.put(series.id(), SeriesDao.insert(db,
                        new Series(0, courseId, withLecturer(series.details(), lecturerIds), series.schedule())));
            }
            for (var session : data.sessions()) {
                var seriesId = mapped(seriesIds, session.seriesId(), "series");
                SessionDao.insert(db, new Session(0, seriesId, session.day(),
                        withLecturer(session.details(), lecturerIds)));
            }
            return null;
        });
    }

    private static long mapped(Map<Long, Long> ids, long oldId, String what) {
        var id = ids.get(oldId);
        if (id == null) throw new IllegalArgumentException("The backup refers to a " + what + " it does not hold: " + oldId);
        return id;
    }

    /** The details with the lecturer id of the data replaced by the one the lecturer got in the database. */
    private static SessionDetails withLecturer(SessionDetails d, Map<Long, Long> lecturerIds) {
        var lecturer = d.lecturerId() == SessionDetails.NO_LECTURER
                ? SessionDetails.NO_LECTURER : mapped(lecturerIds, d.lecturerId(), "lecturer");
        return d.withLecturer(lecturer);
    }

    // --- lecturers ---

    /**
     * Inserts the lecturer when its id is 0, otherwise updates it.
     *
     * @return the lecturer id
     * @throws IllegalArgumentException if the last name is blank
     */
    public static long saveLecturer(SQLiteDatabase db, Lecturer lecturer) {
        var clean = lecturer.normalized();
        if (clean.lastName().isEmpty()) throw new IllegalArgumentException("The last name must not be empty");
        if (clean.id() == 0) return LecturerDao.insert(db, clean);
        require(LecturerDao.get(db, clean.id()), "lecturer", clean.id());
        LecturerDao.update(db, clean);
        return clean.id();
    }

    /** The events and sessions that used the lecturer stay and are left without one. */
    public static void deleteLecturer(SQLiteDatabase db, long id) {
        LecturerDao.delete(db, id);
    }

    // --- semesters ---

    /**
     * Inserts the semester when its id is 0, otherwise updates it. Changing the period of an existing
     * semester also clips or prolongs its series, see {@link SemesterRules#resize}.
     *
     * @return the semester id
     */
    public static long saveSemester(SQLiteDatabase db, Semester semester) {
        var customName = TextSanitizer.lineOrNull(semester.customName(), TextSanitizer.MAX_NAME);
        var clean = new Semester(semester.id(), semester.start(), semester.end(), customName);

        return transact(db, () -> {
            var check = SemesterRules.check(clean, SemesterDao.list(db));
            if (check != SemesterRules.SemesterCheck.OK) {
                throw new IllegalArgumentException("Invalid semester period: " + check);
            }
            if (clean.id() == 0) return SemesterDao.insert(db, clean);

            Semester old = require(SemesterDao.get(db, clean.id()), "semester", clean.id());
            SemesterDao.update(db, clean);
            if (!old.start().equals(clean.start()) || !old.end().equals(clean.end())) {
                apply(db, SemesterRules.resize(old, clean,
                        SeriesDao.listBySemester(db, clean.id()), SessionDao.listBySemester(db, clean.id())));
            }
            return clean.id();
        });
    }

    public static void deleteSemester(SQLiteDatabase db, long id) {
        SemesterDao.delete(db, id);
    }

    // --- courses and series ---

    /**
     * Creates a course together with its series and generates all their sessions.
     *
     * @param series series templates; their ids and course ids are ignored
     * @return the course id
     */
    public static long createCourse(SQLiteDatabase db, Course course, List<Series> series) {
        return transact(db, () -> {
            Semester semester = require(SemesterDao.get(db, course.semesterId()), "semester", course.semesterId());
            var courseId = CourseDao.insert(db, new Course(0, semester.id(), requireName(course.name()),
                    course.color(), cleanLink(course.moodleLink())));
            for (var template : series) insertSeries(db, semester, courseId, template);
            return courseId;
        });
    }

    public static void updateCourse(SQLiteDatabase db, Course course) {
        CourseDao.update(db, new Course(course.id(), course.semesterId(), requireName(course.name()),
                course.color(), cleanLink(course.moodleLink())));
    }

    /**
     * Saves an edited course together with its series as one transaction. Series of the stored course
     * that are missing from {@code drafts} are deleted, drafts with id 0 are added, and drafts with a
     * known id are applied with {@link SeriesEditor#editSeries} if they differ from what is stored.
     */
    public static void saveCourse(SQLiteDatabase db, Course course, List<Series> drafts) {
        transact(db, () -> {
            var stored = loadCourse(db, course.id());
            CourseDao.update(db, new Course(course.id(), stored.course().semesterId(),
                    requireName(course.name()), course.color(), cleanLink(course.moodleLink())));

            var storedById = new HashMap<Long, Series>();
            for (var series : stored.series()) storedById.put(series.id(), series);
            var kept = new HashSet<Long>();
            for (var draft : drafts) {
                if (draft.id() != 0) kept.add(draft.id());
            }
            for (var series : stored.series()) {
                if (!kept.contains(series.id())) SeriesDao.delete(db, series.id());
            }

            for (var draft : drafts) {
                if (draft.id() == 0) {
                    insertSeries(db, stored.semester(), course.id(), draft);
                    continue;
                }
                Series current = require(storedById.get(draft.id()), "series", draft.id());
                // Untouched events are skipped before their text is validated, so an old value that today's
                // rules would refuse (a link written long ago) cannot block saving the rest of the course.
                if (current.details().equals(draft.details()) && current.schedule().equals(draft.schedule())) continue;
                var details = requireValid(db, draft.details());
                apply(db, SeriesEditor.editSeries(stored.semester(), current,
                        SessionDao.listBySeries(db, current.id()), details, draft.schedule()));
            }
            return null;
        });
    }

    public static void deleteCourse(SQLiteDatabase db, long id) {
        CourseDao.delete(db, id);
    }

    /** @return the series id */
    public static long addSeries(SQLiteDatabase db, long courseId, Series series) {
        return transact(db, () -> {
            Course course = require(CourseDao.get(db, courseId), "course", courseId);
            Semester semester = require(SemesterDao.get(db, course.semesterId()), "semester", course.semesterId());
            return insertSeries(db, semester, courseId, series);
        });
    }

    public static void deleteSeries(SQLiteDatabase db, long id) {
        SeriesDao.delete(db, id);
    }

    // --- sessions ---

    /**
     * @param details complete new details of the edited session
     * @param day     new date, used for {@link EditScope#THIS_ONLY}
     * @param schedule new recurrence, used for the series scopes
     * @see SeriesEditor#edit
     */
    public static void editSession(SQLiteDatabase db, long sessionId, EditScope scope,
                                   SessionDetails details, LocalDate day, Schedule schedule) {
        transact(db, () -> {
            var context = loadSessionContext(db, sessionId);
            var sessions = SessionDao.listBySeries(db, context.series().id());
            apply(db, SeriesEditor.edit(context.semester(), context.series(), sessions, sessionId, scope,
                    requireValid(db, details), day, schedule));
            return null;
        });
    }

    public static void deleteSession(SQLiteDatabase db, long sessionId, EditScope scope) {
        transact(db, () -> {
            var context = loadSessionContext(db, sessionId);
            var sessions = SessionDao.listBySeries(db, context.series().id());
            apply(db, SeriesEditor.delete(context.series(), sessions, sessionId, scope));
            return null;
        });
    }

    // --- internals ---

    private static long insertSeries(SQLiteDatabase db, Semester semester, long courseId, Series template) {
        var details = requireValid(db, template.details());
        var schedule = template.schedule();
        if (SemesterRules.checkSchedule(semester, schedule) != SemesterRules.ScheduleCheck.OK) {
            throw new IllegalArgumentException("Schedule does not fit the semester: " + schedule);
        }
        var seriesId = SeriesDao.insert(db, new Series(0, courseId, details, schedule));
        for (var day : Recurrence.occurrences(schedule)) {
            SessionDao.insert(db, new Session(0, seriesId, day, details));
        }
        return seriesId;
    }

    private static void apply(SQLiteDatabase db, ChangeSet changes) {
        for (long id : changes.deletedSeriesIds) SeriesDao.delete(db, id);
        for (long id : changes.deletedSessionIds) SessionDao.delete(db, id);
        for (var series : changes.updatedSeries) SeriesDao.update(db, series);
        for (var session : changes.updatedSessions) SessionDao.update(db, session);
        for (var session : changes.newSessions) SessionDao.insert(db, session);
        for (var created : changes.newSeries) {
            var seriesId = SeriesDao.insert(db, created.series());
            for (var session : created.sessions()) {
                SessionDao.insert(db, new Session(0, seriesId, session.day(), session.details()));
            }
            for (var session : created.adopted()) {
                SessionDao.update(db, new Session(session.id(), seriesId, session.day(), session.details()));
            }
        }
    }

    private static <T> T transact(SQLiteDatabase db, Supplier<T> body) {
        db.beginTransaction();
        try {
            var result = body.get();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Normalizes the details and checks the times. A lecturer that was deleted while the form was open is
     * dropped instead of failing the save, so the rest of the edit still goes through.
     */
    private static SessionDetails requireValid(SQLiteDatabase db, SessionDetails details) {
        var clean = details.normalized();
        if (!clean.hasValidTimes()) throw new IllegalArgumentException("Invalid start or end time");
        if (clean.lecturerId() != SessionDetails.NO_LECTURER && LecturerDao.get(db, clean.lecturerId()) == null) {
            return clean.withLecturer(SessionDetails.NO_LECTURER);
        }
        return clean;
    }

    /** @throws IllegalArgumentException if the link is not a web link, see {@link TextSanitizer#webLink} */
    private static String cleanLink(String link) {
        return TextSanitizer.webLink(link);
    }

    private static String requireName(String name) {
        var clean = TextSanitizer.line(name, TextSanitizer.MAX_NAME);
        if (clean.isEmpty()) throw new IllegalArgumentException("The name must not be empty");
        return clean;
    }

    private static <T> T require(T value, String what, long id) {
        if (value == null) throw new IllegalArgumentException("No " + what + " with id " + id);
        return value;
    }
}
