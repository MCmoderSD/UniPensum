package de.mcmodersd.unipensum;

import android.app.Application;
import android.content.Context;

import de.mcmodersd.unipensum.data.AppSettings;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.backup.BackupManager;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.reminder.ReminderNotifier;
import de.mcmodersd.unipensum.reminder.ReminderScheduler;

public class UniPensumApp extends Application {

    private Database database;
    private TimetableRepository repository;
    private AppSettings settings;
    private BackupManager backups;

    public static UniPensumApp from(Context context) {
        return (UniPensumApp) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        database = Database.open(this);
        repository = new TimetableRepository(database);
        backups = new BackupManager(this, database);
        settings = new AppSettings(this);
        settings.applyTheme();
        ReminderNotifier.createChannel(this);
        // The next reminder follows the data: whatever is saved or imported moves or removes the alarm.
        database.setWriteListener(() -> ReminderScheduler.update(this, false, null));
    }

    public TimetableRepository repository() {
        return repository;
    }
    public Database database() {
        return database;
    }
    public AppSettings settings() {
        return settings;
    }
    public BackupManager backups() {
        return backups;
    }
}