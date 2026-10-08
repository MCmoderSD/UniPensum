package de.mcmodersd.unipensum.domain.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;

public final class SemesterRules {

    public enum SemesterCheck {
        OK,
        /** The end is not after the start. */
        INVALID_RANGE,
        OVERLAPS_OTHER
    }

    public enum ScheduleCheck {
        OK,
        /** First day lies after the last day. */
        EMPTY_RANGE,
        OUTSIDE_SEMESTER
    }

    private SemesterRules() { }

    /** @param others all other semesters; one with the same id as the candidate is ignored */
    public static SemesterCheck check(Semester candidate, List<Semester> others) {
        if (!candidate.end().isAfter(candidate.start())) return SemesterCheck.INVALID_RANGE;
        for (var other : others) {
            if (other.id() == candidate.id()) continue;
            var overlaps = !candidate.start().isAfter(other.end()) && !other.start().isAfter(candidate.end());
            if (overlaps) return SemesterCheck.OVERLAPS_OTHER;
        }
        return SemesterCheck.OK;
    }

    public static boolean isWeekday(LocalDate day) {
        var weekday = day.getDayOfWeek();
        return weekday != DayOfWeek.SATURDAY && weekday != DayOfWeek.SUNDAY;
    }

    /** A session may only be moved to Monday to Friday within its semester. */
    public static boolean isValidMoveTarget(Semester semester, LocalDate day) {
        return isWeekday(day) && semester.contains(day);
    }

    public static ScheduleCheck checkSchedule(Semester semester, Schedule schedule) {
        if (schedule.first().isAfter(schedule.last())) return ScheduleCheck.EMPTY_RANGE;
        if (!semester.contains(schedule.first()) || !semester.contains(schedule.last())) {
            return ScheduleCheck.OUTSIDE_SEMESTER;
        }
        return ScheduleCheck.OK;
    }

    /**
     * Writes needed when a semester changes its period. Shortening removes sessions outside the new
     * period and clips the series; extending the end prolongs every series that ran up to the old
     * end and adds the sessions for the new weeks. Extending the start leaves series untouched.
     * A series whose pattern no longer has any occurrence and that keeps no session is deleted.
     *
     * @param series   all series of the semester's courses
     * @param sessions all sessions of those series
     */
    public static ChangeSet resize(Semester old, Semester updated, List<Series> series, List<Session> sessions) {
        var changes = new ChangeSet();
        var deletedSeries = new HashSet<Long>();

        for (var current : series) {
            var schedule = current.schedule();

            var newFirst = schedule.first();
            if (newFirst.isBefore(updated.start())) {
                // Keep the phase of the chain for intervals above one week.
                newFirst = Recurrence.firstOnOrAfter(schedule, updated.start()).orElse(updated.start());
            }

            var newLast = schedule.last();
            if (newLast.isAfter(updated.end())) {
                newLast = updated.end();
            } else if (newLast.equals(old.end()) && updated.end().isAfter(old.end())) {
                newLast = updated.end();
            }

            var resized = new Schedule(schedule.weekday(), newFirst, newLast, schedule.intervalWeeks());
            var keepsSessions = sessions.stream()
                    .anyMatch(s -> s.seriesId() == current.id() && updated.contains(s.day()));
            if (Recurrence.occurrences(resized).isEmpty() && !keepsSessions) {
                changes.deletedSeriesIds.add(current.id());
                deletedSeries.add(current.id());
                continue;
            }

            if (!resized.equals(schedule)) {
                changes.updatedSeries.add(current.withSchedule(resized));
            }
            if (newLast.isAfter(schedule.last())) {
                for (var day : Recurrence.occurrences(resized)) {
                    if (day.isAfter(schedule.last())) {
                        changes.newSessions.add(new Session(0, current.id(), day, current.details()));
                    }
                }
            }
        }

        for (var session : sessions) {
            if (deletedSeries.contains(session.seriesId())) continue;
            if (!updated.contains(session.day())) changes.deletedSessionIds.add(session.id());
        }
        return changes;
    }
}