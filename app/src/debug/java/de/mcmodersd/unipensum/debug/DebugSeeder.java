package de.mcmodersd.unipensum.debug;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.domain.logic.Reminders;
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
 * <p>
 * With {@code --ei unipensum.debug.reminder_in 4} one more session is added today that begins in 4 minutes and
 * reminds 2 minutes before, with a meeting link and a Moodle link, to try a reminder without waiting for one.
 */
public final class DebugSeeder {

    public static final String EXTRA_SEED = "unipensum.debug.seed";
    public static final String EXTRA_REMINDER_IN = "unipensum.debug.reminder_in";
    private static final int TEST_REMINDER_MIN = 2;
    private static final int TEST_LENGTH_MIN = 60;

    private DebugSeeder() {
    }

    public static void seed(Context context, Intent intent) {
        var reminderIn = intent.getIntExtra(EXTRA_REMINDER_IN, -1);
        UniPensumApp.from(context).database().write(db -> {
            for (var existing : TimetableStore.listSemesters(db)) {
                TimetableStore.deleteSemester(db, existing.id());
            }
            for (var existing : TimetableStore.listLecturers(db)) {
                TimetableStore.deleteLecturer(db, existing.id());
            }
            var weber = lecturer(db, "Anna", "Weber", "anna.weber@uni.example", "+49 30 1234567");
            var koch = lecturer(db, "Michael", "Koch", "m.koch@uni.example", null);
            var lang = lecturer(db, "Sabine", "Lang", null, "+49 30 7654321");
            var neumann = lecturer(db, "Jonas", "Neumann", "j.neumann@uni.example", "+49 30 5550123");
            var brandt = lecturer(db, "Tobias", "Brandt", "t.brandt@uni.example", "+49 30 5550456");
            var sommer = lecturer(db, "Lena", "Sommer", "l.sommer@uni.example", null);

            var start = LocalDate.of(2026, 10, 5);
            var end = SemesterDefaults.lectureEnd(start);
            var semester = TimetableStore.saveSemester(db, new Semester(0, start, end, null));

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
            if (reminderIn >= 0) reminderTest(db, semester, reminderIn);
            return null;
        }, null);
    }

    /** One session today that begins in {@code minutes} minutes, so the reminder for it comes soon. */
    private static void reminderTest(SQLiteDatabase db, long semester, int minutes) {
        var begin = LocalDateTime.now().plusMinutes(minutes);
        var startMin = begin.getHour() * 60 + begin.getMinute();
        if (startMin + TEST_LENGTH_MIN > SessionDetails.MINUTES_PER_DAY) return;
        var details = new SessionDetails(SessionType.LECTURE, startMin, startMin + TEST_LENGTH_MIN,
                Mode.IN_PERSON, true, "Test room", "https://meet.example/test", SessionDetails.NO_LECTURER, null,
                TEST_REMINDER_MIN);
        var day = begin.toLocalDate();
        TimetableStore.createCourse(db, new Course(0, semester, "Reminder test", CourseColor.TEAL,
                "https://moodle.uni.example/test"), List.of(series(day.getDayOfWeek(), day, day, 1, details)));
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
                room, link, lecturerId, null, Reminders.defaultFor(mode));
    }
}
