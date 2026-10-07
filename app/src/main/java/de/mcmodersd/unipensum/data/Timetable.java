package de.mcmodersd.unipensum.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mcmodersd.unipensum.domain.model.Semester;

/** Immutable snapshot of all semesters and sessions, indexed by day for the week grid. */
public final class Timetable {

    public static final Timetable EMPTY = new Timetable(List.of(), List.of());

    private final List<Semester> semesters;
    private final Map<LocalDate, List<SessionView>> byDay = new HashMap<>();

    /** @param sessions expected in display order; that order is kept within each day */
    public Timetable(List<Semester> semesters, List<SessionView> sessions) {
        this.semesters = List.copyOf(semesters);
        for (SessionView view : sessions) {
            byDay.computeIfAbsent(view.session().day(), ignored -> new ArrayList<>()).add(view);
        }
    }

    public List<Semester> semesters() {
        return semesters;
    }

    /** Sessions on that day, ordered by start time; empty if there are none. */
    public List<SessionView> on(LocalDate day) {
        List<SessionView> sessions = byDay.get(day);
        return sessions == null ? Collections.emptyList() : Collections.unmodifiableList(sessions);
    }

    /** Whether a session on {@code from} or later has a reminder. */
    public boolean hasReminderFrom(LocalDate from) {
        for (Map.Entry<LocalDate, List<SessionView>> day : byDay.entrySet()) {
            if (day.getKey().isBefore(from)) continue;
            for (SessionView view : day.getValue()) {
                if (view.session().details().hasReminder()) return true;
            }
        }
        return false;
    }

    /** @return the semester containing that day, or {@code null} outside every semester */
    public Semester semesterAt(LocalDate day) {
        for (Semester semester : semesters) {
            if (semester.contains(day)) return semester;
        }
        return null;
    }
}
