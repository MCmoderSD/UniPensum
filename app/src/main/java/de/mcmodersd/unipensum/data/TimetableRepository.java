package de.mcmodersd.unipensum.data;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.util.List;

import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.data.db.Database.Callback;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;

/**
 * The only door from the UI to the data. Queries are live and refresh after every write; writes
 * run in the background and report on the main thread.
 */
public final class TimetableRepository {

    private final Database database;

    public TimetableRepository(Database database) {
        this.database = database;
    }

    // --- observed ---

    public LiveData<Timetable> timetable() {
        return database.observe(TimetableStore::loadTimetable);
    }

    public LiveData<List<Semester>> semesters() {
        return database.observe(TimetableStore::listSemesters);
    }

    public LiveData<List<CourseWithSeries>> courses(long semesterId) {
        return database.observe(db -> TimetableStore.listCourses(db, semesterId));
    }

    public LiveData<List<Lecturer>> lecturers() {
        return database.observe(TimetableStore::listLecturers);
    }

    // --- one-off reads ---

    public void loadSessionContext(long sessionId, Callback<SessionContext> callback) {
        database.read(db -> TimetableStore.loadSessionContext(db, sessionId), callback);
    }

    public void loadCourse(long courseId, Callback<CourseContext> callback) {
        database.read(db -> TimetableStore.loadCourse(db, courseId), callback);
    }

    public void loadSemester(long semesterId, Callback<Semester> callback) {
        database.read(db -> {
            for (var semester : TimetableStore.listSemesters(db)) {
                if (semester.id() == semesterId) return semester;
            }
            throw new IllegalArgumentException("No semester with id " + semesterId);
        }, callback);
    }

    // --- writes ---

    public void saveSemester(Semester semester, Callback<Long> callback) {
        database.write(db -> TimetableStore.saveSemester(db, semester), callback);
    }

    public void deleteSemester(long id, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.deleteSemester(db, id);
            return null;
        }, callback);
    }

    public void saveLecturer(Lecturer lecturer, Callback<Long> callback) {
        database.write(db -> TimetableStore.saveLecturer(db, lecturer), callback);
    }

    public void deleteLecturer(long id, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.deleteLecturer(db, id);
            return null;
        }, callback);
    }

    public void createCourse(Course course, List<Series> series, Callback<Long> callback) {
        database.write(db -> TimetableStore.createCourse(db, course, series), callback);
    }

    public void updateCourse(Course course, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.updateCourse(db, course);
            return null;
        }, callback);
    }

    public void saveCourse(Course course, List<Series> series, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.saveCourse(db, course, series);
            return null;
        }, callback);
    }

    public void deleteCourse(long id, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.deleteCourse(db, id);
            return null;
        }, callback);
    }

    public void addSeries(long courseId, Series series, Callback<Long> callback) {
        database.write(db -> TimetableStore.addSeries(db, courseId, series), callback);
    }

    public void deleteSeries(long id, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.deleteSeries(db, id);
            return null;
        }, callback);
    }

    public void editSession(long sessionId, EditScope scope, SessionDetails details, LocalDate day,
                            Schedule schedule, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.editSession(db, sessionId, scope, details, day, schedule);
            return null;
        }, callback);
    }

    public void deleteSession(long sessionId, EditScope scope, Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.deleteSession(db, sessionId, scope);
            return null;
        }, callback);
    }
}