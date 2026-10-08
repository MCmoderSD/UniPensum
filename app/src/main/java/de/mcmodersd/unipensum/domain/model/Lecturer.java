package de.mcmodersd.unipensum.domain.model;

import java.util.Objects;

import de.mcmodersd.unipensum.domain.text.TextSanitizer;

/**
 * A person who teaches. Lecturers exist independently of semesters and courses, so one entry can be
 * chosen for any number of events.
 *
 * @param firstName may be empty (data migrated from a single free-text name has none)
 * @param lastName  never empty once {@link #normalized()}
 * @param email     {@code null} if unknown
 * @param phone     {@code null} if unknown
 */
public record Lecturer(long id, String firstName, String lastName, String email, String phone) {

    public Lecturer {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }

    /**
     * Cleans all text with {@link TextSanitizer}: trimmed names, an address without spaces, a phone number
     * of dialable characters; blank e-mail and phone become {@code null}.
     */
    public Lecturer normalized() {
        return new Lecturer(
                id,
                TextSanitizer.line(firstName, TextSanitizer.MAX_NAME),
                TextSanitizer.line(lastName, TextSanitizer.MAX_NAME),
                TextSanitizer.email(email),
                TextSanitizer.phone(phone)
        );
    }

    /** The last name, or first and last name; without a first name both styles show the last name. */
    public String name(NameStyle style) {
        if (style == NameStyle.FULL_NAME && !firstName.isEmpty()) return firstName + " " + lastName;
        return lastName;
    }
}