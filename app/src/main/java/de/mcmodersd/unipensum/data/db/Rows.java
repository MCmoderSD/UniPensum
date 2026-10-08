package de.mcmodersd.unipensum.data.db;

import android.content.ContentValues;
import android.database.Cursor;

import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/** Column mapping shared by the series and session tables, which store the same detail fields. */
final class Rows {

    private Rows() { }

    static void putDetails(ContentValues values, SessionDetails details) {
        values.put("type", details.type().key());
        values.put("start_min", details.startMin());
        values.put("end_min", details.endMin());
        values.put("mode", details.mode().key());
        values.put("hybrid", details.hybrid() ? 1 : 0);
        values.put("room", details.room());
        values.put("link", details.link());
        if (details.lecturerId() > 0) values.put("lecturer_id", details.lecturerId());
        else values.putNull("lecturer_id");
        values.put("note", details.note());
        // NULL is "no reminder", so a column that is added later reads as none until it is filled.
        if (details.hasReminder()) values.put("reminder_min", details.reminderMin());
        else values.putNull("reminder_min");
    }

    static SessionDetails readDetails(Cursor cursor) {
        return new SessionDetails(
                SessionType.fromKey(string(cursor, "type")),
                integer(cursor, "start_min"),
                integer(cursor, "end_min"),
                Mode.fromKey(string(cursor, "mode")),
                integer(cursor, "hybrid") != 0,
                string(cursor, "room"),
                string(cursor, "link"),
                longOrZero(cursor, "lecturer_id"),
                string(cursor, "note"),
                nullableInt(cursor, "reminder_min", SessionDetails.NO_REMINDER)
        );
    }

    static int nullableInt(Cursor cursor, String column, int whenNull) {
        var index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? whenNull : cursor.getInt(index);
    }

    /** {@code NULL} reads as 0, which is how the domain says "no lecturer". */
    static long longOrZero(Cursor cursor, String column) {
        var index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? 0 : cursor.getLong(index);
    }

    static String string(Cursor cursor, String column) {
        var index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    static int integer(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
    }

    static long longValue(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }
}
