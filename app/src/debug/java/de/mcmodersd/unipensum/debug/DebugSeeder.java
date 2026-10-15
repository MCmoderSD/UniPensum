package de.mcmodersd.unipensum.debug;

import static de.mcmodersd.unipensum.domain.model.CourseColor.*;
import static de.mcmodersd.unipensum.domain.model.Mode.*;
import static de.mcmodersd.unipensum.domain.model.SessionDetails.*;
import static de.mcmodersd.unipensum.domain.model.SessionType.*;
import static java.time.DayOfWeek.*;

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
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/**
 * Debug builds only. Replaces all data with a sample semester like a university would plan it: six courses, six
 * lecturers with contact details, and events on the fixed slots 8:00, 9:45, 11:30, 14:00 and 15:45 of 90 minutes
 * each (a double slot makes 3 h 15 min), nothing during the lunch break from 13:00 to 14:00 and nothing that
 * overlaps. These are the data of the store screenshots, see {@code screenshots/PLAN.md}. Triggered with
 * {@code adb shell am start -n de.mcmodersd.unipensum/.ui.MainActivity --ez unipensum.debug.seed true}.
 * <p>
 * With {@code --ei unipensum.debug.reminder_in 4} one more session is added today that begins in 4 minutes and
 * reminds 2 minutes before, with a meeting link and a Moodle link, to try a reminder without waiting for one.
 */
@SuppressWarnings("SpellCheckingInspection")
public final class DebugSeeder {

    public static final String EXTRA_SEED = "unipensum.debug.seed";
    public static final String EXTRA_REMINDER_IN = "unipensum.debug.reminder_in";
    private static final int TEST_REMINDER_MIN = 2;
    private static final int TEST_LENGTH_MIN = 60;

    private DebugSeeder() { }

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

            TimetableStore.createCourse(
                    db, new Course(
                            0, semester, "Analysis I", BLUE,
                            "https://moodle.uni.example/course/view.php?id=101"
                    ), List.of(
                            series(
                                    MONDAY, start, end, 1,
                                    note(
                                            details(LECTURE, 9, 45, 11, 15, IN_PERSON, false, "A1", null, weber),
                                            "Bring the exercise sheet."
                                    )
                            ),
                            series(
                                    THURSDAY, start, end, 1,
                                    details(EXERCISE, 14, 0, 15, 30, IN_PERSON, false, "B3", null, koch)
                            )
                    )
            );
            // Every second week, starting in the week of the screenshots.
            TimetableStore.createCourse(
                    db, new Course(0, semester, "Linear Algebra", GREEN, null), List.of(
                            series(
                                    WEDNESDAY, LocalDate.of(2026, 10, 12), end, 2,
                                    details(EXERCISE, 9, 45, 11, 15, IN_PERSON, false, "C 0.12", null, koch)
                            ),
                            series(
                                    FRIDAY, start, end, 1,
                                    details(LECTURE, 14, 0, 15, 30, IN_PERSON, false, "H1", null, weber)
                            )
                    )
            );
            TimetableStore.createCourse(
                    db, new Course(
                            0, semester, "Algorithms", RED,
                            "https://moodle.uni.example/course/view.php?id=202"
                    ), List.of(
                            series(
                                    TUESDAY, start, end, 1,
                                    reminder(details(LECTURE, 8, 0, 11, 15, IN_PERSON, false, "H1", null, neumann), 15)
                            ),
                            series(
                                    WEDNESDAY, start, end, 1,
                                    details(
                                            LAB, 14, 0, 17, 15, ONLINE, false, null,
                                            "https://meet.example/algorithms", brandt
                                    )
                            )
                    )
            );
            TimetableStore.createCourse(
                    db, new Course(
                            0, semester, "Databases", TEAL,
                            "https://moodle.uni.example/course/view.php?id=303"
                    ), List.of(
                            series(
                                    TUESDAY, start, end, 1,
                                    details(LECTURE, 14, 0, 15, 30, IN_PERSON, false, "H2", null, brandt)
                            ),
                            series(
                                    FRIDAY, start, end, 1,
                                    details(LAB, 8, 0, 9, 30, IN_PERSON, false, "Lab 3", null, brandt)
                            )
                    )
            );
            TimetableStore.createCourse(
                    db, new Course(
                            0, semester, "Software Engineering", VIOLET,
                            "https://moodle.uni.example/course/view.php?id=404"
                    ), List.of(
                            series(
                                    MONDAY, start, end, 1,
                                    details(
                                            LECTURE, 14, 0, 15, 30, IN_PERSON, true, "H3",
                                            "https://meet.example/software-engineering", sommer
                                    )
                            ),
                            series(
                                    FRIDAY, start, end, 1,
                                    details(TUTORIAL, 11, 30, 13, 0, IN_PERSON, false, "Library 2", null, NO_LECTURER)
                            )
                    )
            );
            TimetableStore.createCourse(
                    db, new Course(
                            0, semester, "Physics", ORANGE,
                            "https://moodle.uni.example/course/view.php?id=505"
                    ), List.of(
                            series(
                                    THURSDAY, start, end, 1,
                                    details(
                                            LECTURE, 8, 0, 11, 15, IN_PERSON, true, "A2",
                                            "https://meet.example/physics", lang
                                    )
                            )
                    )
            );
            if (reminderIn >= 0) reminderTest(db, semester, reminderIn);
            return null;
        }, null);
    }

    /** One session today that begins in {@code minutes} minutes, so the reminder for it comes soon. */
    private static void reminderTest(SQLiteDatabase db, long semester, int minutes) {
        var begin = LocalDateTime.now().plusMinutes(minutes);
        var startMin = begin.getHour() * 60 + begin.getMinute();
        if (startMin + TEST_LENGTH_MIN > MINUTES_PER_DAY) return;
        var details = new SessionDetails(
                LECTURE, startMin, startMin + TEST_LENGTH_MIN,
                IN_PERSON, true, "Test room", "https://meet.example/test", NO_LECTURER, null,
                TEST_REMINDER_MIN
        );
        var day = begin.toLocalDate();
        TimetableStore.createCourse(
                db, new Course(
                        0, semester, "Reminder test", TEAL,
                        "https://moodle.uni.example/test"
                ), List.of(series(day.getDayOfWeek(), day, day, 1, details))
        );
    }

    private static long lecturer(SQLiteDatabase db, String firstName, String lastName, String email, String phone) {
        return TimetableStore.saveLecturer(db, new Lecturer(0, firstName, lastName, email, phone));
    }

    private static Series series(DayOfWeek weekday, LocalDate first, LocalDate last, int interval, SessionDetails details) {
        return new Series(0, 0, details, new Schedule(weekday, first, last, interval));
    }

    private static SessionDetails details(SessionType type, int startHour, int startMinute, int endHour, int endMinute,
                                          Mode mode, boolean hybrid, String room, String link, long lecturerId) {
        return new SessionDetails(
                type, startHour * 60 + startMinute, endHour * 60 + endMinute, mode, hybrid,
                room, link, lecturerId, null, Reminders.defaultFor(mode)
        );
    }

    private static SessionDetails note(SessionDetails details, String note) {
        return new SessionDetails(
                details.type(), details.startMin(), details.endMin(), details.mode(), details.hybrid(),
                details.room(), details.link(), details.lecturerId(), note, details.reminderMin()
        );
    }

    private static SessionDetails reminder(SessionDetails details, int minutes) {
        return new SessionDetails(
                details.type(), details.startMin(), details.endMin(), details.mode(), details.hybrid(),
                details.room(), details.link(), details.lecturerId(), details.note(), minutes
        );
    }
}