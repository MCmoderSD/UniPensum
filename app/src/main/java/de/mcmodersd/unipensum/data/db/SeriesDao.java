package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Series;

public final class SeriesDao {

    private SeriesDao() {
    }

    public static long insert(SQLiteDatabase db, Series series) {
        return db.insertOrThrow("series", null, values(series));
    }

    public static void update(SQLiteDatabase db, Series series) {
        db.update("series", values(series), "id = ?", new String[]{String.valueOf(series.id())});
    }

    public static void delete(SQLiteDatabase db, long id) {
        db.delete("series", "id = ?", new String[]{String.valueOf(id)});
    }

    /** @return {@code null} if there is no such series */
    public static Series get(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.query("series", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    /** Ordered by weekday, then start time. */
    public static List<Series> listByCourse(SQLiteDatabase db, long courseId) {
        List<Series> result = new ArrayList<>();
        try (Cursor cursor = db.query("series", null, "course_id = ?", new String[]{String.valueOf(courseId)},
                null, null, "weekday, start_min, id")) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    public static List<Series> listBySemester(SQLiteDatabase db, long semesterId) {
        List<Series> result = new ArrayList<>();
        String sql = "SELECT r.* FROM series r JOIN course c ON r.course_id = c.id "
                + "WHERE c.semester_id = ? ORDER BY r.id";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(semesterId)})) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    private static ContentValues values(Series series) {
        ContentValues values = new ContentValues();
        values.put("course_id", series.courseId());
        values.put("weekday", series.schedule().weekday().getValue());
        values.put("first_day", series.schedule().first().toEpochDay());
        values.put("last_day", series.schedule().last().toEpochDay());
        values.put("interval_weeks", series.schedule().intervalWeeks());
        Rows.putDetails(values, series.details());
        return values;
    }

    private static Series read(Cursor cursor) {
        Schedule schedule = new Schedule(
                DayOfWeek.of(Rows.integer(cursor, "weekday")),
                LocalDate.ofEpochDay(Rows.longValue(cursor, "first_day")),
                LocalDate.ofEpochDay(Rows.longValue(cursor, "last_day")),
                Rows.integer(cursor, "interval_weeks"));
        return new Series(
                Rows.longValue(cursor, "id"),
                Rows.longValue(cursor, "course_id"),
                Rows.readDetails(cursor),
                schedule);
    }
}
