package de.mcmodersd.unipensum.data.db;

/**
 * Name and version of the database. The layout is {@code res/raw/schema.sql}: dates are
 * {@code LocalDate.toEpochDay()}, times are minutes since midnight and enums are stored as their stable text keys,
 * so the file stays readable for a later import/export.
 * <p>
 * {@link #VERSION} is 1 from the first public version on; the versions before it were snapshots, which are not
 * migrated but installed anew. A later change of the layout raises it, adds a step to {@code DbHelper.onUpgrade}
 * and has to keep the backups of the older layout importable in {@code BackupJson}.
 */
public final class Schema {

    public static final String NAME = "unipensum.db";
    public static final int VERSION = 1;

    private Schema() { }
}