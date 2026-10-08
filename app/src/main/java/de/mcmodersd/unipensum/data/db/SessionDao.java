package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.ArrayList;

import de.mcmodersd.unipensum.domain.model.Session;

public final class SessionDao {

    private SessionDao() { }

    public static void insert(SQLiteDatabase db, Session session) {
        db.insertOrThrow("session", null, values(session));
    }

    /** Also rewrites {@code series_id}, which is how a session moves into a split-off series. */
    public static void update(SQLiteDatabase db, Session session) {
        db.update("session", values(session), "id = ?", new String[]{String.valueOf(session.id())});
    }

    public static void delete(SQLiteDatabase db, long id) {
        db.delete("session", "id = ?", new String[]{String.valueOf(id)});
    }

    /** @return {@code null} if there is no such session */
    public static Session get(SQLiteDatabase db, long id) {
        try (var cursor = db.query(
                "session", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null
        )) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    /** Ordered by date. */
    public static ArrayList<Session> listBySeries(SQLiteDatabase db, long seriesId) {
        var result = new ArrayList<Session>();
        try (var cursor = db.query(
                "session", null, "series_id = ?", new String[]{String.valueOf(seriesId)},
                null, null, "day, id"
        )) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    public static ArrayList<Session> listBySemester(SQLiteDatabase db, long semesterId) {
        var result = new ArrayList<Session>();
        var sql = "SELECT s.* FROM session s JOIN series r ON s.series_id = r.id "
                + "JOIN course c ON r.course_id = c.id WHERE c.semester_id = ? ORDER BY s.day, s.id";
        try (var cursor = db.rawQuery(sql, new String[]{String.valueOf(semesterId)})) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    /** Reads a session from a cursor that exposes the columns of the {@code session} table. */
    public static Session read(Cursor cursor) {
        return new Session(
                Rows.longValue(cursor, "id"),
                Rows.longValue(cursor, "series_id"),
                LocalDate.ofEpochDay(Rows.longValue(cursor, "day")),
                Rows.readDetails(cursor)
        );
    }

    private static ContentValues values(Session session) {
        var values = new ContentValues();
        values.put("series_id", session.seriesId());
        values.put("day", session.day().toEpochDay());
        Rows.putDetails(values, session.details());
        return values;
    }
}