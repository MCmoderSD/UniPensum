package de.mcmodersd.unipensum.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.logic.Recurrence;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;

/** Shared test data. Dates: 2026-10-05 is a Monday, 2027-02-12 is a Friday. */
public final class Fixtures {

    public static final Semester SEMESTER =
            new Semester(1, LocalDate.of(2026, 10, 5), LocalDate.of(2027, 2, 12), null);

    private Fixtures() {
    }

    public static LocalDate date(int month, int day) {
        int year = month >= 9 ? 2026 : 2027;
        return LocalDate.of(year, month, day);
    }

    /** The lecturer every fixture session has. */
    public static final long LECTURER = 7;

    /** Lecture, 08:00 to 11:00, in person, room A1, lecturer {@link #LECTURER}. */
    public static SessionDetails details() {
        return new SessionDetails(SessionType.LECTURE, 8 * 60, 11 * 60, Mode.IN_PERSON, false,
                "A1", null, LECTURER, null);
    }

    public static SessionDetails withRoom(SessionDetails d, String room) {
        return new SessionDetails(d.type(), d.startMin(), d.endMin(), d.mode(), d.hybrid(),
                room, d.link(), d.lecturerId(), d.note());
    }

    public static SessionDetails withNote(SessionDetails d, String note) {
        return new SessionDetails(d.type(), d.startMin(), d.endMin(), d.mode(), d.hybrid(),
                d.room(), d.link(), d.lecturerId(), note);
    }

    public static SessionDetails withLecturer(SessionDetails d, long lecturerId) {
        return new SessionDetails(d.type(), d.startMin(), d.endMin(), d.mode(), d.hybrid(),
                d.room(), d.link(), lecturerId, d.note());
    }

    public static Schedule weekly(DayOfWeek weekday, LocalDate first, LocalDate last) {
        return new Schedule(weekday, first, last, 1);
    }

    public static Series series(long id, Schedule schedule) {
        return new Series(id, 1, details(), schedule);
    }

    /** One session per occurrence, ids counting up from {@code firstSessionId}. */
    public static List<Session> sessionsOf(Series series, long firstSessionId) {
        List<Session> result = new ArrayList<>();
        long id = firstSessionId;
        for (LocalDate day : Recurrence.occurrences(series.schedule())) {
            result.add(new Session(id++, series.id(), day, series.details()));
        }
        return result;
    }

    public static Session byId(List<Session> sessions, long id) {
        for (Session session : sessions) {
            if (session.id() == id) return session;
        }
        throw new AssertionError("No session " + id);
    }
}
