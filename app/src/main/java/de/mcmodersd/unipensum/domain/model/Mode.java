package de.mcmodersd.unipensum.domain.model;

public enum Mode {
    IN_PERSON("in_person"),
    ONLINE("online");

    private final String key;

    Mode(String key) {
        this.key = key;
    }

    /** Stable key for persistence and later import/export. */
    public String key() {
        return key;
    }

    public static Mode fromKey(String key) {
        for (Mode value : values()) {
            if (value.key.equals(key)) return value;
        }
        throw new IllegalArgumentException("Unknown mode: " + key);
    }
}
