package de.mcmodersd.unipensum.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.data.db.DbHelper;
import de.mcmodersd.unipensum.domain.backup.BackupCleaner;
import de.mcmodersd.unipensum.domain.backup.BackupData;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/** The database side of a backup: what is exported, and replacing everything with it. */
@RunWith(AndroidJUnit4.class)
public class BackupStoreTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2027, 1, 22);

    private DbHelper helperA;
    private DbHelper helperB;
    private SQLiteDatabase a;
    private SQLiteDatabase b;

    @Before
    public void openDatabases() {
        Context context = ApplicationProvider.getApplicationContext();
        helperA = new DbHelper(context, null);
        helperB = new DbHelper(context, null);
        a = helperA.getWritableDatabase();
        b = helperB.getWritableDatabase();
    }

    @After
    public void closeDatabases() {
        helperA.close();
        helperB.close();
    }

    /** Lecturers, a semester, a course with two events, one session moved to another day and room. */
    private static void fill(SQLiteDatabase db, String courseName) {
        var weber = TimetableStore.saveLecturer(db, new Lecturer(0, "Anna", "Weber", "anna@uni.example", "030 123"));
        var koch = TimetableStore.saveLecturer(db, new Lecturer(0, "", "Prof. Koch", null, null));
        var semester = TimetableStore.saveSemester(db, new Semester(0, START, END, "Winter term"));
        var lecture = new SessionDetails(
                SessionType.LECTURE, 480, 675, Mode.IN_PERSON, true,
                "A1", "https://meet.example/x", weber, "bring laptop", SessionDetails.NO_REMINDER
        );
        var exercise = new SessionDetails(
                SessionType.EXERCISE, 600, 700, Mode.ONLINE, false,
                null, "https://meet.example/y", koch, null, SessionDetails.NO_REMINDER
        );
        TimetableStore.createCourse(
                db, new Course(0, semester, courseName, CourseColor.TEAL, "https://moodle.example/c"),
                List.of(
                        new Series(0, 0, lecture, new Schedule(DayOfWeek.MONDAY, START, END, 1)),
                        new Series(0, 0, exercise, new Schedule(DayOfWeek.THURSDAY, START, END, 2))
                )
        );
        var third = TimetableStore.loadTimetable(db).on(START.plusWeeks(2)).get(0).session();
        var moved = new SessionDetails(
                SessionType.LECTURE, 480, 675, Mode.IN_PERSON, true,
                "B7", "https://meet.example/x", weber, "bring laptop", SessionDetails.NO_REMINDER
        );
        TimetableStore.editSession(db, third.id(), EditScope.THIS_ONLY, moved, START.plusWeeks(2).plusDays(1), null);
    }

    /**
     * Everything in the database as text, without any id, with the lecturers by name: two databases with
     * the same signature hold the same timetable even though their ids differ.
     */
    private static List<String> signature(SQLiteDatabase db) {
        var lines = new ArrayList<String>();
        var lecturerNames = new HashMap<Long, String>();
        for (var l : TimetableStore.listLecturers(db)) {
            lecturerNames.put(l.id(), l.firstName() + " " + l.lastName());
            lines.add("L|" + l.firstName() + "|" + l.lastName() + "|" + l.email() + "|" + l.phone());
        }
        LocalDate first = null;
        LocalDate last = null;
        for (var s : TimetableStore.listSemesters(db)) {
            lines.add("S|" + s.start() + "|" + s.end() + "|" + s.customName());
            if (first == null || s.start().isBefore(first)) first = s.start();
            if (last == null || s.end().isAfter(last)) last = s.end();
            for (var c : TimetableStore.listCourses(db, s.id())) {
                lines.add(
                        "C|" + s.start() + "|" + c.course().name() + "|" + c.course().color() + "|"
                                + c.course().moodleLink()
                );
                for (var r : c.series()) {
                    var d = r.details();
                    lines.add(
                            "R|" + c.course().name() + "|" + r.schedule() + "|" + d.type() + "|" + d.startMin()
                                    + "|" + d.endMin() + "|" + d.mode() + "|" + d.hybrid() + "|" + d.room() + "|" + d.link()
                                    + "|" + (d.lecturerId() == 0 ? "-" : lecturerNames.get(d.lecturerId())) + "|" + d.note()
                    );
                }
            }
        }
        if (first != null) {
            var timetable = TimetableStore.loadTimetable(db);
            for (var day = first; !day.isAfter(last); day = day.plusDays(1)) {
                for (var v : timetable.on(day)) {
                    var d = v.session().details();
                    lines.add(
                            "X|" + day + "|" + v.courseName() + "|" + d.type() + "|" + d.startMin() + "|" + d.endMin()
                                    + "|" + d.mode() + "|" + d.hybrid() + "|" + d.room() + "|" + d.link() + "|"
                                    + (v.lecturer() == null ? "-" : v.lecturer().lastName()) + "|" + d.note()
                    );
                }
            }
        }
        return lines;
    }

    private static BackupData empty() {
        return new BackupData(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    public void whatIsExportedFromOneDatabase_isTheSameTimetableInAnother() {
        fill(a, "Math");
        fill(b, "Old course");               // b has other data, with ids that differ from a's

        TimetableStore.replaceAll(b, TimetableStore.exportData(a));

        assertEquals(signature(a), signature(b));
        assertFalse(signature(b).isEmpty());
    }

    @Test
    public void replacingRemovesEverythingThatWasThere() {
        fill(b, "Old course");

        fill(a, "Math");
        TimetableStore.replaceAll(b, TimetableStore.exportData(a));

        for (var line : signature(b)) assertFalse(line, line.contains("Old course"));
        assertEquals(2, TimetableStore.listLecturers(b).size());          // not 4
        assertEquals(1, TimetableStore.listSemesters(b).size());
        assertEquals(TimetableStore.exportData(a).sessions().size(), DatabaseUtils.queryNumEntries(b, "session"));
    }

    @Test
    public void replacingWithNothing_emptiesTheDatabase() {
        fill(b, "Old course");

        TimetableStore.replaceAll(b, empty());

        assertEquals(0, DatabaseUtils.queryNumEntries(b, "semester"));
        assertEquals(0, DatabaseUtils.queryNumEntries(b, "course"));
        assertEquals(0, DatabaseUtils.queryNumEntries(b, "series"));
        assertEquals(0, DatabaseUtils.queryNumEntries(b, "session"));
        assertEquals(0, DatabaseUtils.queryNumEntries(b, "lecturer"));
    }

    @Test
    public void theIdsAreNewButTheyTieTheRowsTogether() {
        fill(a, "Math");
        fill(b, "Old course");                // advances b's counters, so its new ids differ from a's
        TimetableStore.replaceAll(b, TimetableStore.exportData(a));

        var exported = TimetableStore.exportData(b);
        var checked = BackupCleaner.clean(exported, 0, 0);

        // Nothing is left out or adjusted: every course finds its semester, every session its event.
        assertTrue(checked.report().isClean());
        assertEquals(exported, checked.data());
        // b had a semester before, and the counters of the ids do not go back.
        assertTrue(exported.semesters().get(0).id() != TimetableStore.exportData(a).semesters().get(0).id());
    }

    @Test
    public void importedEventsCanBeEditedAndDeletedLikeAnyOther() {
        fill(a, "Math");
        TimetableStore.replaceAll(b, TimetableStore.exportData(a));
        var before = DatabaseUtils.queryNumEntries(b, "session");

        var monday = TimetableStore.loadTimetable(b).on(START).get(0).session();
        TimetableStore.deleteSession(b, monday.id(), EditScope.ALL);

        assertTrue(DatabaseUtils.queryNumEntries(b, "session") < before);
        assertEquals(1, DatabaseUtils.queryNumEntries(b, "series"));
    }

    @Test
    public void ifSomethingGoesWrong_theOldDataIsStillThere() {
        fill(b, "Old course");
        var before = signature(b);

        fill(a, "Math");
        var good = TimetableStore.exportData(a);
        // The last series names a course that is not in the data, so the import fails after the early rows.
        var series = new ArrayList<Series>(good.series());
        var orphan = series.remove(series.size() - 1);
        series.add(new Series(orphan.id(), 424242, orphan.details(), orphan.schedule()));
        var broken = new BackupData(good.lecturers(), good.semesters(), good.courses(), series, good.sessions());

        assertThrows(IllegalArgumentException.class, () -> TimetableStore.replaceAll(b, broken));

        assertEquals(before, signature(b));
    }
}
