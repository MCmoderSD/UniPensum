package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private SeriesEditor() {
    }

    /**
     * @param sessions all sessions of {@code series}
     * @param day      new date of the edited session, used for {@link EditScope#THIS_ONLY}
     * @param schedule new recurrence, used for the series scopes. For
     *                 {@link EditScope#THIS_AND_FOLLOWING} its first day is raised to the edited session's date.
     * @throws IllegalArgumentException if the date or schedule violates the semester rules
     */
    public static ChangeSet edit(Semester semester, Series series, List<Session> sessions, long sessionId,
                                 EditScope scope, SessionDetails details, LocalDate day, Schedule schedule) {
        Session target = find(sessions, sessionId);
        switch (scope) {
            case THIS_ONLY:
                return editOne(semester, target, details, day);
            case THIS_AND_FOLLOWING:
                if (!isFirst(sessions, target)) {
                    return editFollowing(semester, series, sessions, target, details, schedule);
                }
                return editAll(semester, series, sessions, target.details(), details, schedule);
            case ALL:
                return editAll(semester, series, sessions, target.details(), details, schedule);
            default:
                throw new IllegalArgumentException("Unknown scope: " + scope);
        }
    }

    /**
     * Edits a series as a whole without going through one of its sessions, as the course editor does.
     * The series template is the baseline: fields that differ from it are carried over to every session,
     * fields that stay as they are keep each session's individual value.
     */
    public static ChangeSet editSeries(Semester semester, Series series, List<Session> sessions,
                                       SessionDetails details, Schedule schedule) {
        return editAll(semester, series, sessions, series.details(), details, schedule);
    }

    public static ChangeSet delete(Series series, List<Session> sessions, long sessionId, EditScope scope) {
        Session target = find(sessions, sessionId);
        ChangeSet changes = new ChangeSet();
        switch (scope) {
            case THIS_ONLY:
                changes.deletedSessionIds.add(target.id());
                break;
            case THIS_AND_FOLLOWING:
                if (isFirst(sessions, target)) {
                    changes.deletedSeriesIds.add(series.id());
                } else {
                    for (Session following : followingOf(sessions, target)) {
                        changes.deletedSessionIds.add(following.id());
                    }
                    changes.updatedSeries.add(series.withSchedule(endBefore(series.schedule(), target)));
                }
                break;
            case ALL:
                changes.deletedSeriesIds.add(series.id());
                break;
            default:
                throw new IllegalArgumentException("Unknown scope: " + scope);
        }
        return changes;
    }

    private static ChangeSet editOne(Semester semester, Session target, SessionDetails details, LocalDate day) {
        Objects.requireNonNull(day, "day");
        if (!SemesterRules.isValidMoveTarget(semester, day)) {
            throw new IllegalArgumentException("Not a valid date for this semester: " + day);
        }
        ChangeSet changes = new ChangeSet();
        if (!day.equals(target.day()) || !details.equals(target.details())) {
            changes.updatedSessions.add(new Session(target.id(), target.seriesId(), day, details));
        }
        return changes;
    }

    /** @param before the details the edit started from, the baseline for "which fields changed" */
    private static ChangeSet editAll(Semester semester, Series series, List<Session> sessions, SessionDetails before,
                                     SessionDetails details, Schedule schedule) {
        requireValidSchedule(semester, schedule);
        SessionDetails template = carryOver(before, details, series.details());
        Series updated = new Series(series.id(), series.courseId(), template, schedule);

        ChangeSet changes = new ChangeSet();
        if (schedule.equals(series.schedule())) {
            for (Session session : sessions) {
                SessionDetails carried = carryOver(before, details, session.details());
                if (!carried.equals(session.details())) changes.updatedSessions.add(session.withDetails(carried));
            }
            if (!updated.equals(series)) changes.updatedSeries.add(updated);
        } else {
            changes.updatedSeries.add(updated);
            for (Session session : sessions) changes.deletedSessionIds.add(session.id());
            for (LocalDate date : Recurrence.occurrences(schedule)) {
                changes.newSessions.add(new Session(0, series.id(), date, template));
            }
        }
        return changes;
    }

    private static ChangeSet editFollowing(Semester semester, Series series, List<Session> sessions, Session target,
                                           SessionDetails details, Schedule schedule) {
        requireValidSchedule(semester, schedule);
        Schedule old = series.schedule();
        SessionDetails before = target.details();
        SessionDetails template = carryOver(before, details, series.details());
        List<Session> following = followingOf(sessions, target);

        boolean sameRhythm = schedule.weekday() == old.weekday()
                && schedule.last().equals(old.last())
                && schedule.intervalWeeks() == old.intervalWeeks()
                && !schedule.first().isAfter(target.day());

        LocalDate newFirst = schedule.first().isBefore(target.day()) ? target.day() : schedule.first();
        if (sameRhythm) {
            newFirst = Recurrence.firstOnOrAfter(old, newFirst).orElse(newFirst);
        }
        Schedule split = schedule.withFirst(newFirst);
        if (split.first().isAfter(split.last())) {
            throw new IllegalArgumentException("The new series would be empty");
        }

        ChangeSet changes = new ChangeSet();
        changes.updatedSeries.add(series.withSchedule(endBefore(old, target)));

        List<Session> generated = new ArrayList<>();
        List<Session> adopted = new ArrayList<>();
        if (sameRhythm) {
            for (Session session : following) {
                adopted.add(session.withDetails(carryOver(before, details, session.details())));
            }
        } else {
            for (Session session : following) changes.deletedSessionIds.add(session.id());
            for (LocalDate date : Recurrence.occurrences(split)) {
                generated.add(new Session(0, 0, date, template));
            }
        }
        changes.newSeries.add(new ChangeSet.NewSeries(
                new Series(0, series.courseId(), template, split), generated, adopted));
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

    private static Session find(List<Session> sessions, long sessionId) {
        for (Session session : sessions) {
            if (session.id() == sessionId) return session;
        }
        throw new IllegalArgumentException("Session " + sessionId + " is not part of the series");
    }

    private static boolean isFirst(List<Session> sessions, Session target) {
        for (Session session : sessions) {
            if (BY_DAY_THEN_ID.compare(session, target) < 0) return false;
        }
        return true;
    }

    private static List<Session> followingOf(List<Session> sessions, Session target) {
        List<Session> result = new ArrayList<>();
        for (Session session : sessions) {
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
                pick(before.reminderMin(), after.reminderMin(), base.reminderMin()));
    }

    private static <T> T pick(T before, T after, T base) {
        return Objects.equals(before, after) ? base : after;
    }
}
