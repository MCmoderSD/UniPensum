package de.mcmodersd.unipensum.domain.model;

public enum SessionType {
    LECTURE("lecture"),
    EXERCISE("exercise"),
    LAB("lab"),
    TUTORIAL("tutorial");

    private final String key;

    SessionType(String key) {
        this.key = key;
    }

    /** Stable key for persistence and later import/export. */
    public String key() {
        return key;
    }

    public static SessionType fromKey(String key) {
        for (var value : values()) {
            if (value.key.equals(key)) return value;
        }
        throw new IllegalArgumentException("Unknown session type: " + key);
    }
}
