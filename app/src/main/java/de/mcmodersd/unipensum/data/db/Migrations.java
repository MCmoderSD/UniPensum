package de.mcmodersd.unipensum.data.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * Version ladder. {@code STEPS[0]} migrates version 1 to 2, {@code STEPS[1]} version 2 to 3, and so on.
 * Add a step here together with the bump of {@link Schema#VERSION}; never edit an existing step.
 * <p>
 * Backups ({@code data/backup}) record {@link Schema#VERSION} and must stay importable for as long as
 * people keep them. A schema change therefore also has to be handled in {@code BackupJson} (new fields
 * optional with a default, old fields still understood) and gets a test with a fixed sample of the
 * previous form in {@code BackupFileTest}.
 */
final class Migrations {

    interface Step {
        void apply(SQLiteDatabase db);
    }

    private static final Step[] STEPS = {
            Migrations::lecturersAndMoodleLink,
            Migrations::reminders
    };

    private Migrations() {
    }

    static void upgrade(SQLiteDatabase db, int from, int to) {
        for (int version = from; version < to; version++) {
            int index = version - 1;
            if (index < 0 || index >= STEPS.length) {
                throw new IllegalStateException("No migration from version " + version + " to " + (version + 1));
            }
            STEPS[index].apply(db);
        }
    }

    /**
     * Every event and session gets a reminder: 5 minutes before the start for an online one, 30 for the rest.
     * The same defaults as {@code Reminders.defaultFor}, written out here because a migration must not change
     * when the code does.
     */
    private static void reminders(SQLiteDatabase db) {
        for (String table : new String[]{"series", "session"}) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN reminder_min INTEGER");
            db.execSQL("UPDATE " + table + " SET reminder_min = CASE mode WHEN 'online' THEN 5 ELSE 30 END");
        }
    }

    /**
     * Version 1 stored the lecturer as free text on every series and session. Each distinct text becomes
     * a lecturer whose last name is that text, and the rows point at it. The old text columns stay in the
     * table, emptied: {@code DROP COLUMN} needs SQLite 3.35 (Android 12 and 13 ship 3.32), and rebuilding
     * the tables would cascade-delete the sessions while foreign keys are on.
     */
    private static void lecturersAndMoodleLink(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE lecturer ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "first_name TEXT NOT NULL, "
                + "last_name TEXT NOT NULL, "
                + "email TEXT, "
                + "phone TEXT)");
        db.execSQL("ALTER TABLE course ADD COLUMN moodle_link TEXT");
        db.execSQL("ALTER TABLE series ADD COLUMN lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL");
        db.execSQL("ALTER TABLE session ADD COLUMN lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL");

        db.execSQL("INSERT INTO lecturer (first_name, last_name) "
                + "SELECT '', name FROM ("
                + "SELECT TRIM(lecturer) AS name FROM series WHERE lecturer IS NOT NULL "
                + "UNION SELECT TRIM(lecturer) FROM session WHERE lecturer IS NOT NULL) "
                + "WHERE name <> '' ORDER BY name");

        for (String table : new String[]{"series", "session"}) {
            db.execSQL("UPDATE " + table + " SET lecturer_id = ("
                    + "SELECT l.id FROM lecturer l WHERE l.first_name = '' AND l.last_name = TRIM(" + table + ".lecturer)) "
                    + "WHERE lecturer IS NOT NULL AND TRIM(lecturer) <> ''");
            db.execSQL("UPDATE " + table + " SET lecturer = NULL");
        }

        db.execSQL("CREATE INDEX series_lecturer ON series(lecturer_id)");
        db.execSQL("CREATE INDEX session_lecturer ON session(lecturer_id)");
    }
}
