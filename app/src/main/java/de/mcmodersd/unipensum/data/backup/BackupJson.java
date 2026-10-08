package de.mcmodersd.unipensum.data.backup;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import de.mcmodersd.unipensum.domain.backup.BackupData;
import de.mcmodersd.unipensum.domain.logic.Reminders;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/**
 * The data of a backup as JSON: five lists, one entry per row, dates as {@code 2026-10-05}, times as
 * minutes since midnight, enums as their stable keys, empty values left out. A file without the reminders
 * (from before they existed) is read with the default reminder of each event.
 * <p>
 * Reading is as forgiving as it can be, because the file may come from another version: unknown fields
 * and lists are ignored, a missing optional value takes its default, an unknown enum value is replaced
 * by the default (and counted), and an entry that cannot be understood is skipped (and counted) instead
 * of failing the whole file. What is read is not trusted yet: {@code BackupCleaner} checks it next.
 */
final class BackupJson {

    /** @param skipped  entries that could not be read at all
     *  @param adjusted values that were replaced by a default */
    record Parsed(BackupData data, int skipped, int adjusted) {
    }

    private BackupJson() { }

    // --- writing ---

    static void write(BackupData data, OutputStream out) throws IOException {
        var json = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        json.beginObject();

        json.name("lecturers").beginArray();
        for (var lecturer : data.lecturers()) {
            json.beginObject();
            json.name("id").value(lecturer.id());
            json.name("firstName").value(lecturer.firstName());
            json.name("lastName").value(lecturer.lastName());
            optional(json, "email", lecturer.email());
            optional(json, "phone", lecturer.phone());
            json.endObject();
        }
        json.endArray();

        json.name("semesters").beginArray();
        for (var semester : data.semesters()) {
            json.beginObject();
            json.name("id").value(semester.id());
            json.name("start").value(semester.start().toString());
            json.name("end").value(semester.end().toString());
            optional(json, "name", semester.customName());
            json.endObject();
        }
        json.endArray();

        json.name("courses").beginArray();
        for (var course : data.courses()) {
            json.beginObject();
            json.name("id").value(course.id());
            json.name("semester").value(course.semesterId());
            json.name("name").value(course.name());
            json.name("color").value(course.color().key());
            optional(json, "moodle", course.moodleLink());
            json.endObject();
        }
        json.endArray();

        json.name("series").beginArray();
        for (var series : data.series()) {
            json.beginObject();
            json.name("id").value(series.id());
            json.name("course").value(series.courseId());
            json.name("weekday").value(series.schedule().weekday().getValue());
            json.name("first").value(series.schedule().first().toString());
            json.name("last").value(series.schedule().last().toString());
            json.name("interval").value(series.schedule().intervalWeeks());
            writeDetails(json, series.details());
            json.endObject();
        }
        json.endArray();

        json.name("sessions").beginArray();
        for (var session : data.sessions()) {
            json.beginObject();
            json.name("id").value(session.id());
            json.name("series").value(session.seriesId());
            json.name("day").value(session.day().toString());
            writeDetails(json, session.details());
            json.endObject();
        }
        json.endArray();

        json.endObject();
        json.flush();
    }

    private static void writeDetails(JsonWriter json, SessionDetails details) throws IOException {
        json.name("type").value(details.type().key());
        json.name("startMin").value(details.startMin());
        json.name("endMin").value(details.endMin());
        json.name("mode").value(details.mode().key());
        if (details.hybrid()) json.name("hybrid").value(true);
        optional(json, "room", details.room());
        optional(json, "link", details.link());
        if (details.lecturerId() != SessionDetails.NO_LECTURER) json.name("lecturer").value(details.lecturerId());
        optional(json, "note", details.note());
        // Always written, "none" as -1: a file without the field is older and gets the default reminder.
        json.name("reminder").value(details.reminderMin());
    }

    private static void optional(JsonWriter json, String name, String value) throws IOException {
        if (value != null) json.name(name).value(value);
    }

    // --- reading ---

    /** @throws IOException if the text is not JSON; {@link IllegalStateException} if it is not an object */
    static Parsed read(InputStream in) throws IOException {
        return new Reader().read(new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
    }

    private static final class Reader {

        private int skipped;
        private int adjusted;

        Parsed read(JsonReader json) throws IOException {
            List<Lecturer> lecturers = new ArrayList<>();
            List<Semester> semesters = new ArrayList<>();
            List<Course> courses = new ArrayList<>();
            List<Series> series = new ArrayList<>();
            List<Session> sessions = new ArrayList<>();

            json.beginObject();
            while (json.hasNext()) {
                switch (json.nextName()) {
                    case "lecturers":
                        lecturers = array(json, this::lecturer);
                        break;
                    case "semesters":
                        semesters = array(json, this::semester);
                        break;
                    case "courses":
                        courses = array(json, this::course);
                        break;
                    case "series":
                        series = array(json, this::series);
                        break;
                    case "sessions":
                        sessions = array(json, this::session);
                        break;
                    default:
                        json.skipValue();           // a list a newer version added
                        break;
                }
            }
            json.endObject();
            return new Parsed(new BackupData(lecturers, semesters, courses, series, sessions), skipped, adjusted);
        }

        /** Reads a list of objects; one that cannot be turned into a record is skipped and counted. */
        private <T> List<T> array(JsonReader json, Function<Map<String, Object>, T> parse) throws IOException {
            var result = new ArrayList<T>();
            if (json.peek() != JsonToken.BEGIN_ARRAY) {
                json.skipValue();
                return result;
            }
            json.beginArray();
            while (json.hasNext()) {
                if (json.peek() != JsonToken.BEGIN_OBJECT) {
                    json.skipValue();
                    skipped++;
                    continue;
                }
                var fields = object(json);
                try {
                    result.add(parse.apply(fields));
                } catch (RuntimeException unreadable) {
                    // A missing field, a malformed date or number, a weekday that does not exist.
                    skipped++;
                }
            }
            json.endArray();
            return result;
        }

        /** Reads one object into a map of its plain values; nested values are skipped. */
        private static Map<String, Object> object(JsonReader json) throws IOException {
            var fields = new HashMap<String, Object>();
            json.beginObject();
            while (json.hasNext()) {
                var name = json.nextName();
                switch (json.peek()) {
                    case STRING:
                    case NUMBER:
                        fields.put(name, json.nextString());
                        break;
                    case BOOLEAN:
                        fields.put(name, json.nextBoolean());
                        break;
                    case NULL:
                        json.nextNull();
                        break;
                    default:
                        json.skipValue();
                        break;
                }
            }
            json.endObject();
            return fields;
        }

        private Lecturer lecturer(Map<String, Object> f) {
            return new Lecturer(requiredLong(f, "id"), orEmpty(text(f, "firstName")),
                    requiredText(f, "lastName"), text(f, "email"), text(f, "phone"));
        }

        private Semester semester(Map<String, Object> f) {
            return new Semester(requiredLong(f, "id"), LocalDate.parse(requiredText(f, "start")),
                    LocalDate.parse(requiredText(f, "end")), text(f, "name"));
        }

        private Course course(Map<String, Object> f) {
            // The required fields first: a default only counts as an adjustment if the entry is kept.
            var id = requiredLong(f, "id");
            var semester = requiredLong(f, "semester");
            var name = requiredText(f, "name");
            CourseColor color;
            try {
                color = CourseColor.fromKey(text(f, "color"));
            } catch (IllegalArgumentException unknown) {
                color = CourseColor.BLUE;
                adjusted++;
            }
            return new Course(id, semester, name, color, text(f, "moodle"));
        }

        private Series series(Map<String, Object> f) {
            var schedule = new Schedule(DayOfWeek.of((int) requiredLong(f, "weekday")),
                    LocalDate.parse(requiredText(f, "first")), LocalDate.parse(requiredText(f, "last")),
                    f.containsKey("interval") ? (int) requiredLong(f, "interval") : 1);
            return new Series(requiredLong(f, "id"), requiredLong(f, "course"), details(f), schedule);
        }

        private Session session(Map<String, Object> f) {
            return new Session(requiredLong(f, "id"), requiredLong(f, "series"),
                    LocalDate.parse(requiredText(f, "day")), details(f));
        }

        private SessionDetails details(Map<String, Object> f) {
            var startMin = (int) requiredLong(f, "startMin");
            var endMin = (int) requiredLong(f, "endMin");
            var lecturer = f.containsKey("lecturer") ? requiredLong(f, "lecturer") : SessionDetails.NO_LECTURER;
            SessionType type;
            try {
                type = SessionType.fromKey(text(f, "type"));
            } catch (IllegalArgumentException unknown) {
                type = SessionType.LECTURE;
                adjusted++;
            }
            Mode mode;
            try {
                mode = Mode.fromKey(text(f, "mode"));
            } catch (IllegalArgumentException unknown) {
                mode = Mode.IN_PERSON;
                adjusted++;
            }
            return new SessionDetails(type, startMin, endMin, mode, Boolean.TRUE.equals(f.get("hybrid")),
                    text(f, "room"), text(f, "link"), lecturer, text(f, "note"), reminder(f, mode));
        }

        /** A file from before the reminders has no field and gets the default; so does a value that is no number. */
        private int reminder(Map<String, Object> f, Mode mode) {
            if (!f.containsKey("reminder")) return Reminders.defaultFor(mode);
            try {
                return (int) Math.max(SessionDetails.NO_REMINDER,
                        Math.min(SessionDetails.MAX_REMINDER_MIN, requiredLong(f, "reminder")));
            } catch (RuntimeException notANumber) {
                adjusted++;
                return Reminders.defaultFor(mode);
            }
        }

        private static String text(Map<String, Object> f, String key) {
            var value = f.get(key);
            return value instanceof String ? (String) value : null;
        }

        private static String orEmpty(String value) {
            return value == null ? "" : value;
        }

        private static String requiredText(Map<String, Object> f, String key) {
            var value = text(f, key);
            if (value == null) throw new IllegalArgumentException("Missing " + key);
            return value;
        }

        private static long requiredLong(Map<String, Object> f, String key) {
            return Long.parseLong(requiredText(f, key));
        }
    }
}
