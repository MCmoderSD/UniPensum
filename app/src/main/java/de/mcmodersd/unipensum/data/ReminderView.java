package de.mcmodersd.unipensum.data;

import de.mcmodersd.unipensum.domain.model.Session;

/**
 * A session with a reminder and what the reminder shows besides it: the name of the course and its Moodle
 * link, which the session does not carry itself.
 *
 * @param moodleLink {@code null} if the course has none
 */
public record ReminderView(Session session, String courseName, String moodleLink) {
}