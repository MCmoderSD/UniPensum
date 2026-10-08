package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Semester;

public final class SemesterDao {

    private SemesterDao() { }

    public static long insert(SQLiteDatabase db, Semester semester) {
        return db.insertOrThrow("semester", null, values(semester));
    }

    public static void update(SQLiteDatabase db, Semester semester) {
        db.update("semester", values(semester), "id = ?", new String[]{String.valueOf(semester.id())});
    }

    public static void delete(SQLiteDatabase db, long id) {
        db.delete("semester", "id = ?", new String[]{String.valueOf(id)});
    }

    /** Removes every semester and, through the foreign keys, all courses, series and sessions. */
    public static void deleteAll(SQLiteDatabase db) {
        db.delete("semester", null, null);
    }

    /** @return {@code null} if there is no such semester */
    public static Semester get(SQLiteDatabase db, long id) {
        try (var cursor = db.query(
                "semester", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null
        )) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    /** Ordered by start date. */
    public static List<Semester> list(SQLiteDatabase db) {
        var result = new ArrayList<Semester>();
        try (var cursor = db.query("semester", null, null, null, null, null, "start_day, id")) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    private static ContentValues values(Semester semester) {
        var values = new ContentValues();
        values.put("start_day", semester.start().toEpochDay());
        values.put("end_day", semester.end().toEpochDay());
        values.put("custom_name", semester.customName());
        return values;
    }

    private static Semester read(Cursor cursor) {
        return new Semester(
                Rows.longValue(cursor, "id"),
                LocalDate.ofEpochDay(Rows.longValue(cursor, "start_day")),
                LocalDate.ofEpochDay(Rows.longValue(cursor, "end_day")),
                Rows.string(cursor, "custom_name")
        );
    }
}
