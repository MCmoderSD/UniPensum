package de.mcmodersd.unipensum.domain.model;

/**
 * Fixed preset palette for courses. Only the key is persisted; the actual light and dark
 * colors are resolved in the UI layer, so the palette can change without a migration.
 */
public enum CourseColor {
    RED("red"),
    ORANGE("orange"),
    YELLOW("yellow"),
    GREEN("green"),
    TEAL("teal"),
    BLUE("blue"),
    VIOLET("violet"),
    PINK("pink"),
    GRAPHITE("graphite"),
    GRAY("gray"),
    SILVER("silver");

    private final String key;

    CourseColor(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static CourseColor fromKey(String key) {
        for (var value : values()) {
            if (value.key.equals(key)) return value;
        }
        throw new IllegalArgumentException("Unknown course color: " + key);
    }
}
