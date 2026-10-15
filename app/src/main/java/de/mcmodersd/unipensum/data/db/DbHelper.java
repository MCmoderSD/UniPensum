package de.mcmodersd.unipensum.data.db;

import static de.mcmodersd.unipensum.R.raw.schema;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class DbHelper extends SQLiteOpenHelper {

    private final Resources resources;

    /** @param name file name, or {@code null} for a throwaway in-memory database (tests) */
    public DbHelper(Context context, String name) {
        super(context, name, null, Schema.VERSION);
        resources = context.getResources();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (var statement : SqlScript.statements(readSchema())) {
            db.execSQL(statement);
        }
    }

    /**
     * Version 1 is the first one, so there is nothing to upgrade from yet. The first change of the layout
     * adds its migration here.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("No migration from version " + oldVersion + " to " + newVersion);
    }

    /** The layout is {@code res/raw/schema.sql}, where an editor can highlight it. */
    private String readSchema() {
        try (var stream = resources.openRawResource(schema)) {
            var reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException unreadable) {
            throw new IllegalStateException("Cannot read the schema", unreadable);
        }
    }
}