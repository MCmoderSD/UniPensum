package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;

public final class CourseDao {

    private CourseDao() { }

    public static long insert(SQLiteDatabase db, Course course) {
        return db.insertOrThrow("course", null, values(course));
    }

    public static void update(SQLiteDatabase db, Course course) {
        db.update("course", values(course), "id = ?", new String[]{String.valueOf(course.id())});
    }

    public static void delete(SQLiteDatabase db, long id) {
        db.delete("course", "id = ?", new String[]{String.valueOf(id)});
    }

    /** @return {@code null} if there is no such course */
    public static Course get(SQLiteDatabase db, long id) {
        try (var cursor = db.query(
                "course", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null
        )) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    /** Ordered by name, case-insensitive. */
    public static List<Course> listBySemester(SQLiteDatabase db, long semesterId) {
        var result = new ArrayList<Course>();
        try (var cursor = db.query(
                "course", null, "semester_id = ?", new String[]{String.valueOf(semesterId)},
                null, null, "name COLLATE NOCASE, id"
        )) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    private static ContentValues values(Course course) {
        var values = new ContentValues();
        values.put("semester_id", course.semesterId());
        values.put("name", course.name());
        values.put("color", course.color().key());
        values.put("moodle_link", course.moodleLink());
        return values;
    }

    private static Course read(Cursor cursor) {
        return new Course(
                Rows.longValue(cursor, "id"),
                Rows.longValue(cursor, "semester_id"),
                Rows.string(cursor, "name"),
                CourseColor.fromKey(Rows.string(cursor, "color")),
                Rows.string(cursor, "moodle_link")
        );
    }
}