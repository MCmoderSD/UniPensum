package de.mcmodersd.unipensum.data.db;

/**
 * Database layout. Dates are {@code LocalDate.toEpochDay()}, times are minutes since midnight and
 * enums are stored as their stable text keys, so the file stays readable for a later import/export.
 */
public final class Schema {

    public static final String NAME = "unipensum.db";
    public static final int VERSION = 3;

    static final String[] CREATE_STATEMENTS = {
            "CREATE TABLE semester ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "start_day INTEGER NOT NULL, "
                    + "end_day INTEGER NOT NULL, "
                    + "custom_name TEXT)",

            "CREATE TABLE lecturer ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "first_name TEXT NOT NULL, "
                    + "last_name TEXT NOT NULL, "
                    + "email TEXT, "
                    + "phone TEXT)",

            "CREATE TABLE course ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "semester_id INTEGER NOT NULL REFERENCES semester(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, "
                    + "color TEXT NOT NULL, "
                    + "moodle_link TEXT)",

            "CREATE TABLE series ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "course_id INTEGER NOT NULL REFERENCES course(id) ON DELETE CASCADE, "
                    + "type TEXT NOT NULL, "
                    + "weekday INTEGER NOT NULL, "
                    + "start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, "
                    + "mode TEXT NOT NULL, "
                    + "hybrid INTEGER NOT NULL, "
                    + "room TEXT, "
                    + "link TEXT, "
                    + "lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL, "
                    + "note TEXT, "
                    + "reminder_min INTEGER, "
                    + "first_day INTEGER NOT NULL, "
                    + "last_day INTEGER NOT NULL, "
                    + "interval_weeks INTEGER NOT NULL)",

            "CREATE TABLE session ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "series_id INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE, "
                    + "day INTEGER NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "start_min INTEGER NOT NULL, "
                    + "end_min INTEGER NOT NULL, "
                    + "mode TEXT NOT NULL, "
                    + "hybrid INTEGER NOT NULL, "
                    + "room TEXT, "
                    + "link TEXT, "
                    + "lecturer_id INTEGER REFERENCES lecturer(id) ON DELETE SET NULL, "
                    + "note TEXT, "
                    + "reminder_min INTEGER)",

            "CREATE INDEX course_semester ON course(semester_id)",
            "CREATE INDEX series_course ON series(course_id)",
            "CREATE INDEX series_lecturer ON series(lecturer_id)",
            "CREATE INDEX session_series ON session(series_id)",
            "CREATE INDEX session_day ON session(day)",
            "CREATE INDEX session_lecturer ON session(lecturer_id)"
    };

    private Schema() { }
}