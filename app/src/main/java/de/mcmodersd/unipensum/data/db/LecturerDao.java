package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Lecturer;

public final class LecturerDao {

    private LecturerDao() { }

    public static long insert(SQLiteDatabase db, Lecturer lecturer) {
        return db.insertOrThrow("lecturer", null, values(lecturer));
    }

    public static void update(SQLiteDatabase db, Lecturer lecturer) {
        db.update("lecturer", values(lecturer), "id = ?", new String[]{String.valueOf(lecturer.id())});
    }

    /** Series and sessions that used the lecturer are left without one (foreign key {@code SET NULL}). */
    public static void delete(SQLiteDatabase db, long id) {
        db.delete("lecturer", "id = ?", new String[]{String.valueOf(id)});
    }

    public static void deleteAll(SQLiteDatabase db) {
        db.delete("lecturer", null, null);
    }

    /** @return {@code null} if there is no such lecturer */
    public static Lecturer get(SQLiteDatabase db, long id) {
        try (var cursor = db.query("lecturer", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? read(cursor, "") : null;
        }
    }

    /** Ordered by last name, then first name, case-insensitive. */
    public static List<Lecturer> list(SQLiteDatabase db) {
        var result = new ArrayList<Lecturer>();
        try (var cursor = db.query("lecturer", null, null, null, null, null,
                "last_name COLLATE NOCASE, first_name COLLATE NOCASE, id")) {
            while (cursor.moveToNext()) result.add(read(cursor, ""));
        }
        return result;
    }

    /**
     * Reads a lecturer from a cursor that exposes the lecturer columns under a prefix, as the joined
     * queries do ({@code lec_id}, {@code lec_first_name}, ...). Pass {@code ""} for the table itself.
     *
     * @return {@code null} if the row has no lecturer (a {@code LEFT JOIN} without a match)
     */
    public static Lecturer read(Cursor cursor, String prefix) {
        if (cursor.isNull(cursor.getColumnIndexOrThrow(prefix + "id"))) return null;
        return new Lecturer(
                Rows.longValue(cursor, prefix + "id"),
                Rows.string(cursor, prefix + "first_name"),
                Rows.string(cursor, prefix + "last_name"),
                Rows.string(cursor, prefix + "email"),
                Rows.string(cursor, prefix + "phone"));
    }

    private static ContentValues values(Lecturer lecturer) {
        var values = new ContentValues();
        values.put("first_name", lecturer.firstName());
        values.put("last_name", lecturer.lastName());
        values.put("email", lecturer.email());
        values.put("phone", lecturer.phone());
        return values;
    }
}
