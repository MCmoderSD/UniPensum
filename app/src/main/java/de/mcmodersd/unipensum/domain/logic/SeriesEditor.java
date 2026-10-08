package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;

/**
 * Turns an edit or deletion of one session into a {@link ChangeSet}, depending on its {@link EditScope}.
 * <p>
 * Edits are passed as the complete new {@link SessionDetails}. The editor diffs them against the
 * edited session and only carries the fields that actually changed over to the other sessions of the
 * series, so individual deviations in other fields survive. Changing the {@link Schedule} itself
 * regenerates the affected sessions, which discards their individual edits.
 */
public final class SeriesEditor {

    private static final Comparator<Session> BY_DAY_THEN_ID =
            Comparator.comparing(Session::day).thenComparingLong(Session::id);

    private SeriesEditor() { }

    /**
     * @param sessions all sessions of {@code series}
     * @param day      new date of the edited session, used for {@link EditScope#THIS_ONLY}
     * @param schedule new recurrence, used for the series scopes. For
     *                 {@link EditScope#THIS_AND_FOLLOWING} its first day is raised to the edited session's date.
     * @throws IllegalArgumentException if the date or schedule violates the semester rules
     */
    public static ChangeSet edit(Semester semester, Series series, ArrayList<Session> sessions, long sessionId,
                                 EditScope scope, SessionDetails details, LocalDate day, Schedule schedule) {
        var target = find(sessions, sessionId);
        return switch (scope) {
            case THIS_ONLY -> editOne(semester, target, details, day);
            case THIS_AND_FOLLOWING -> {
                if (!isFirst(sessions, target)) {
                    yield editFollowing(semester, series, sessions, target, details, schedule);
                }
                yield editAll(semester, series, sessions, target.details(), details, schedule);
            }
            case ALL -> editAll(semester, series, sessions, target.details(), details, schedule);
        };
    }

    /**
     * Edits a series as a whole without going through one of its sessions, as the course editor does.
     * The series template is the baseline: fields that differ from it are carried over to every session,
     * fields that stay as they are keep each session's individual value.
     */
    public static ChangeSet editSeries(Semester semester, Series series, ArrayList<Session> sessions,
                                       SessionDetails details, Schedule schedule) {
        return editAll(semester, series, sessions, series.details(), details, schedule);
    }

    public static ChangeSet delete(Series series, ArrayList<Session> sessions, long sessionId, EditScope scope) {
        var target = find(sessions, sessionId);
        var changes = new ChangeSet();
        switch (scope) {
            case THIS_ONLY -> changes.deletedSessionIds.add(target.id());
            case THIS_AND_FOLLOWING -> {
                if (isFirst(sessions, target)) {
                    changes.deletedSeriesIds.add(series.id());
                } else {
                    for (var following : followingOf(sessions, target)) {
                        changes.deletedSessionIds.add(following.id());
                    }
                    changes.updatedSeries.add(series.withSchedule(endBefore(series.schedule(), target)));
                }
            }
            case ALL -> changes.deletedSeriesIds.add(series.id());
        }
        return changes;
    }

    private static ChangeSet editOne(Semester semester, Session target, SessionDetails details, LocalDate day) {
        Objects.requireNonNull(day, "day");
        if (!SemesterRules.isValidMoveTarget(semester, day)) {
            throw new IllegalArgumentException("Not a valid date for this semester: " + day);
        }
        var changes = new ChangeSet();
        if (!day.equals(target.day()) || !details.equals(target.details())) {
            changes.updatedSessions.add(new Session(target.id(), target.seriesId(), day, details));
        }
        return changes;
    }

    /** @param before the details the edit started from, the baseline for "which fields changed" */
    private static ChangeSet editAll(Semester semester, Series series, ArrayList<Session> sessions, SessionDetails before,
                                     SessionDetails details, Schedule schedule) {
        requireValidSchedule(semester, schedule);
        var template = carryOver(before, details, series.details());
        var updated = new Series(series.id(), series.courseId(), template, schedule);

        var changes = new ChangeSet();
        if (schedule.equals(series.schedule())) {
            for (var session : sessions) {
                var carried = carryOver(before, details, session.details());
                if (!carried.equals(session.details())) changes.updatedSessions.add(session.withDetails(carried));
            }
            if (!updated.equals(series)) changes.updatedSeries.add(updated);
        } else {
            changes.updatedSeries.add(updated);
            for (var session : sessions) changes.deletedSessionIds.add(session.id());
            for (var date : Recurrence.occurrences(schedule)) {
                changes.newSessions.add(new Session(0, series.id(), date, template));
            }
        }
        return changes;
    }

    private static ChangeSet editFollowing(Semester semester, Series series, ArrayList<Session> sessions, Session target,
                                           SessionDetails details, Schedule schedule) {
        requireValidSchedule(semester, schedule);
        var old = series.schedule();
        var before = target.details();
        var template = carryOver(before, details, series.details());
        var following = followingOf(sessions, target);

        var sameRhythm = schedule.weekday() == old.weekday()
                && schedule.last().equals(old.last())
                && schedule.intervalWeeks() == old.intervalWeeks()
                && !schedule.first().isAfter(target.day());

        var newFirst = schedule.first().isBefore(target.day()) ? target.day() : schedule.first();
        if (sameRhythm) {
            newFirst = Recurrence.firstOnOrAfter(old, newFirst).orElse(newFirst);
        }
        var split = schedule.withFirst(newFirst);
        if (split.first().isAfter(split.last())) {
            throw new IllegalArgumentException("The new series would be empty");
        }

        var changes = new ChangeSet();
        changes.updatedSeries.add(series.withSchedule(endBefore(old, target)));

        var generated = new ArrayList<Session>();
        var adopted = new ArrayList<Session>();
        if (sameRhythm) {
            for (var session : following) {
                adopted.add(session.withDetails(carryOver(before, details, session.details())));
            }
        } else {
            for (var session : following) changes.deletedSessionIds.add(session.id());
            for (var date : Recurrence.occurrences(split)) {
                generated.add(new Session(0, 0, date, template));
            }
        }
        changes.newSeries.add(
                new ChangeSet.NewSeries(
                        new Series(0, series.courseId(), template, split), generated, adopted
                )
        );
        return changes;
    }

    private static void requireValidSchedule(Semester semester, Schedule schedule) {
        Objects.requireNonNull(schedule, "schedule");
        if (SemesterRules.checkSchedule(semester, schedule) != SemesterRules.ScheduleCheck.OK) {
            throw new IllegalArgumentException("Schedule does not fit the semester: " + schedule);
        }
    }

    /** The old series stops the day before the split. */
    private static Schedule endBefore(Schedule schedule, Session target) {
        return schedule.withLast(target.day().minusDays(1));
    }

    private static Session find(ArrayList<Session> sessions, long sessionId) {
        for (var session : sessions) {
            if (session.id() == sessionId) return session;
        }
        throw new IllegalArgumentException("Session " + sessionId + " is not part of the series");
    }

    private static boolean isFirst(ArrayList<Session> sessions, Session target) {
        for (var session : sessions) {
            if (BY_DAY_THEN_ID.compare(session, target) < 0) return false;
        }
        return true;
    }

    private static ArrayList<Session> followingOf(ArrayList<Session> sessions, Session target) {
        var result = new ArrayList<Session>();
        for (var session : sessions) {
            if (BY_DAY_THEN_ID.compare(session, target) >= 0) result.add(session);
        }
        result.sort(BY_DAY_THEN_ID);
        return result;
    }

    /**
     * Takes every field from {@code after} that differs between {@code before} and {@code after},
     * and keeps the rest of {@code base}.
     */
    private static SessionDetails carryOver(SessionDetails before, SessionDetails after, SessionDetails base) {
        return new SessionDetails(
                pick(before.type(), after.type(), base.type()),
                pick(before.startMin(), after.startMin(), base.startMin()),
                pick(before.endMin(), after.endMin(), base.endMin()),
                pick(before.mode(), after.mode(), base.mode()),
                pick(before.hybrid(), after.hybrid(), base.hybrid()),
                pick(before.room(), after.room(), base.room()),
                pick(before.link(), after.link(), base.link()),
                pick(before.lecturerId(), after.lecturerId(), base.lecturerId()),
                pick(before.note(), after.note(), base.note()),
                pick(before.reminderMin(), after.reminderMin(), base.reminderMin())
        );
    }

    private static <T> T pick(T before, T after, T base) {
        return Objects.equals(before, after) ? base : after;
    }
}