package de.mcmodersd.unipensum.debug;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.domain.logic.SemesterDefaults;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/**
 * Debug builds only. Replaces all data with a sample semester, including two overlapping courses and
 * lecturers with contact details. Triggered with
 * {@code adb shell am start -n de.mcmodersd.unipensum/.ui.MainActivity --ez unipensum.debug.seed true}.
 */
public final class DebugSeeder {

    public static final String EXTRA_SEED = "unipensum.debug.seed";

    private DebugSeeder() {
    }

    public static void seed(Context context) {
        UniPensumApp.from(context).database().write(db -> {
            for (Semester existing : TimetableStore.listSemesters(db)) {
                TimetableStore.deleteSemester(db, existing.id());
            }
            for (Lecturer existing : TimetableStore.listLecturers(db)) {
                TimetableStore.deleteLecturer(db, existing.id());
            }
            long weber = lecturer(db, "Anna", "Weber", "anna.weber@uni.example", "+49 30 1234567");
            long koch = lecturer(db, "Michael", "Koch", "m.koch@uni.example", null);
            long lang = lecturer(db, "Sabine", "Lang", null, "+49 30 7654321");
            long neumann = lecturer(db, "Jonas", "Neumann", "j.neumann@uni.example", "+49 30 5550123");
            long brandt = lecturer(db, "Tobias", "Brandt", "t.brandt@uni.example", "+49 30 5550456");
            long sommer = lecturer(db, "Lena", "Sommer", "l.sommer@uni.example", null);

            LocalDate start = LocalDate.of(2026, 10, 5);
            LocalDate end = SemesterDefaults.lectureEnd(start);
            long semester = TimetableStore.saveSemester(db, new Semester(0, start, end, null));

            TimetableStore.createCourse(db, new Course(0, semester, "Analysis", CourseColor.BLUE,
                    "https://moodle.uni.example/course/view.php?id=101"), List.of(
                    series(DayOfWeek.MONDAY, start, end, 1,
                            details(SessionType.LECTURE, 8, 0, 10, 30, Mode.IN_PERSON, false, "A1", null, weber)),
                    series(DayOfWeek.THURSDAY, start, end, 2,
                            details(SessionType.EXERCISE, 13, 0, 14, 30, Mode.IN_PERSON, false, "B3", null, koch))));
            // Overlaps the Analysis lecture on Mondays from 09:30 to 10:30.
            TimetableStore.createCourse(db, new Course(0, semester, "Linear Algebra", CourseColor.GREEN, null), List.of(
                    series(DayOfWeek.MONDAY, start, end, 1,
                            details(SessionType.EXERCISE, 9, 30, 11, 0, Mode.IN_PERSON, false, "C 0.12", null, lang))));
            TimetableStore.createCourse(db, new Course(0, semester, "Algorithms", CourseColor.RED,
                    "moodle.uni.example/course/view.php?id=202"), List.of(
                    series(DayOfWeek.TUESDAY, start, end, 1,
                            details(SessionType.LECTURE, 10, 0, 12, 0, Mode.IN_PERSON, false, "H1", null, neumann)),
                    series(DayOfWeek.WEDNESDAY, start, end, 1,
                            details(SessionType.LAB, 14, 0, 16, 0, Mode.ONLINE, false, null, "https://meet.example/algo", brandt))));
            TimetableStore.createCourse(db, new Course(0, semester, "Physics", CourseColor.ORANGE, null), List.of(
                    series(DayOfWeek.FRIDAY, start, end, 1,
                            details(SessionType.LECTURE, 9, 0, 12, 0, Mode.IN_PERSON, true, "A2", "https://meet.example/physics", sommer))));
            TimetableStore.createCourse(db, new Course(0, semester, "Seminar", CourseColor.GRAPHITE, null), List.of(
                    series(DayOfWeek.WEDNESDAY, start, end, 1,
                            details(SessionType.TUTORIAL, 11, 0, 12, 30, Mode.IN_PERSON, false, "Library 2", null,
                                    SessionDetails.NO_LECTURER))));
            return null;
        }, null);
    }

    private static long lecturer(SQLiteDatabase db, String firstName, String lastName, String email, String phone) {
        return TimetableStore.saveLecturer(db, new Lecturer(0, firstName, lastName, email, phone));
    }

    private static Series series(DayOfWeek weekday, LocalDate first, LocalDate last, int interval, SessionDetails details) {
        return new Series(0, 0, details, new Schedule(weekday, first, last, interval));
    }

    private static SessionDetails details(SessionType type, int startHour, int startMinute, int endHour, int endMinute,
                                          Mode mode, boolean hybrid, String room, String link, long lecturerId) {
        return new SessionDetails(type, startHour * 60 + startMinute, endHour * 60 + endMinute, mode, hybrid,
                room, link, lecturerId, null);
    }
}
