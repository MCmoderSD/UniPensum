package de.mcmodersd.unipensum.domain.model;

import java.util.Objects;

import de.mcmodersd.unipensum.domain.text.TextSanitizer;

/**
 * Everything about a session except when it happens. Shared by {@link Series} (as the template)
 * and {@link Session} (as the per-occurrence copy), so a single session can deviate from its series.
 *
 * @param startMin minutes since midnight
 * @param endMin   minutes since midnight, exclusive
 * @param hybrid     only meaningful for {@link Mode#IN_PERSON}
 * @param lecturerId id of a {@link Lecturer}, {@link #NO_LECTURER} if there is none
 */
public record SessionDetails(
        SessionType type,
        int startMin,
        int endMin,
        Mode mode,
        boolean hybrid,
        String room,
        String link,
        long lecturerId,
        String note) {

    public static final int MINUTES_PER_DAY = 24 * 60;
    public static final long NO_LECTURER = 0;

    public SessionDetails {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mode, "mode");
    }

    public boolean hasValidTimes() {
        return startMin >= 0 && startMin < endMin && endMin <= MINUTES_PER_DAY;
    }

    /**
     * Drops values that do not apply to the chosen mode (a room for online, a link for plain
     * in-person), cleans the text with {@link TextSanitizer} and turns blank text into {@code null}.
     *
     * @throws IllegalArgumentException if a link that is kept is not a web link, see
     *                                  {@link TextSanitizer#webLink}; check with {@link TextSanitizer#isValidWebLink} first
     */
    public SessionDetails normalized() {
        boolean online = mode == Mode.ONLINE;
        boolean hybridEffective = !online && hybrid;
        return new SessionDetails(
                type,
                startMin,
                endMin,
                mode,
                hybridEffective,
                online ? null : TextSanitizer.lineOrNull(room, TextSanitizer.MAX_ROOM),
                (online || hybridEffective) ? TextSanitizer.webLink(link) : null,
                lecturerId > 0 ? lecturerId : NO_LECTURER,
                TextSanitizer.text(note, TextSanitizer.MAX_NOTE));
    }
}
