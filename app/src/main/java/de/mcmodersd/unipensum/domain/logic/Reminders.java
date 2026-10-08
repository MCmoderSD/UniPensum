package de.mcmodersd.unipensum.domain.logic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Session;

/**
 * The rules of the reminders, without anything of Android: how long before a session the app reminds, which
 * reminders are due, and which one comes next. Times are local date-times; the time zone is the caller's.
 */
public final class Reminders {

    /** A reminder of an event on site, also a hybrid one. */
    public static final int IN_PERSON_DEFAULT_MIN = 30;
    /** A reminder of an event that is only online: the way to the room is not needed. */
    public static final int ONLINE_DEFAULT_MIN = 5;
    /** The reminder is set in steps of this many minutes. */
    public static final int STEP_MIN = 5;

    public record Due(Session session, LocalDateTime remindAt, LocalDateTime start, LocalDateTime end) { }

    private static final Comparator<Due> BY_TIME =
            Comparator.comparing(Due::remindAt).thenComparing(due -> due.session().id());

    private Reminders() { }

    public static int defaultFor(Mode mode) {
        return mode == Mode.ONLINE ? ONLINE_DEFAULT_MIN : IN_PERSON_DEFAULT_MIN;
    }

    /**
     * The reminder after the format of an event was changed: a time that is still the default of the old format
     * moves to the default of the new one, a time the user chose, or no reminder, stays.
     */
    public static int afterModeChange(int reminderMin, Mode from, Mode to) {
        return reminderMin == defaultFor(from) ? defaultFor(to) : reminderMin;
    }

    /** @return the reminder of the session, empty if it has none */
    public static Optional<Due> of(Session session) {
        var details = session.details();
        if (!details.hasReminder()) return Optional.empty();
        var start = session.day().atStartOfDay().plusMinutes(details.startMin());
        var end = session.day().atStartOfDay().plusMinutes(details.endMin());
        return Optional.of(new Due(session, start.minusMinutes(details.reminderMin()), start, end));
    }

    /**
     * The reminders that were due after {@code after} (exclusive) up to {@code now} (inclusive) and are still of
     * use, which is until their session is over. Earliest first.
     */
    public static ArrayList<Due> due(List<Session> sessions, LocalDateTime after, LocalDateTime now) {
        var result = new ArrayList<Due>();
        for (var session : sessions) {
            of(session).ifPresent(due -> {
                if (due.remindAt().isAfter(after) && !due.remindAt().isAfter(now) && due.end().isAfter(now)) {
                    result.add(due);
                }
            });
        }
        result.sort(BY_TIME);
        return result;
    }

    /** @return the time of the first reminder after {@code now}, empty if none is to come */
    public static Optional<LocalDateTime> next(List<Session> sessions, LocalDateTime now) {
        LocalDateTime next = null;
        for (var session : sessions) {
            var due = of(session);
            if (due.isEmpty() || !due.get().remindAt().isAfter(now)) continue;
            if (next == null || due.get().remindAt().isBefore(next)) next = due.get().remindAt();
        }
        return Optional.ofNullable(next);
    }
}