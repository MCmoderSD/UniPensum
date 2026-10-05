package de.mcmodersd.unipensum.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DbHelper extends SQLiteOpenHelper {

    /** @param name file name, or {@code null} for a throwaway in-memory database (tests) */
    public DbHelper(Context context, String name) {
        super(context, name, null, Schema.VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String statement : Schema.CREATE_STATEMENTS) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Migrations.upgrade(db, oldVersion, newVersion);
    }
}
