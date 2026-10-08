package de.mcmodersd.unipensum.data.backup;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.data.db.Schema;
import de.mcmodersd.unipensum.domain.backup.BackupCleaner;
import de.mcmodersd.unipensum.domain.backup.BackupData;

/**
 * Export and import for the UI. The files are reached through the document dialogs of the system (the
 * caller hands in a {@link Uri}), so the app needs no storage permission. Reading, writing and
 * especially the key derivation of a protected backup run on their own thread; results are reported on
 * the main thread like everything else in the data layer.
 * <p>
 * An import takes three steps so the user can see what is in the file before it replaces anything:
 * {@link #open} reads the manifest, {@link #load} decrypts and checks the data, {@link #replaceAll}
 * writes it.
 * <p>
 * Passwords are passed as {@code char[]} and cleared as soon as they have been used; pass a fresh array
 * for each call.
 */
public final class BackupManager {

    /** The versions of the installed app, to compare a backup's {@code BackupInfo} with. */
    public record Current(int format, int schema, int appVersionCode, String appVersion) {
    }

    private final Context context;
    private final Database database;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "unipensum-backup");
        thread.setDaemon(true);
        return thread;
    });

    public BackupManager(Context context, Database database) {
        this.context = context.getApplicationContext();
        this.database = database;
    }

    public Current current() {
        try {
            var info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return new Current(BackupFile.FORMAT, Schema.VERSION, (int) info.getLongVersionCode(),
                    info.versionName == null ? "" : info.versionName);
        } catch (PackageManager.NameNotFoundException ownPackage) {
            return new Current(BackupFile.FORMAT, Schema.VERSION, 0, "");
        }
    }

    /**
     * Writes all data to {@code target}, protected with {@code password} unless it is {@code null}.
     */
    public void export(Uri target, char[] password, Database.Callback<Void> callback) {
        database.read(TimetableStore::exportData, new Database.Callback<BackupData>() {
            @Override
            public void onSuccess(BackupData data) {
                worker.execute(() -> {
                    try {
                        var current = current();
                        var meta = new BackupFile.Meta(current.schema(), current.appVersionCode(),
                                current.appVersion(), Instant.now());
                        var file = BackupFile.write(data, meta, password);
                        try (var out = context.getContentResolver().openOutputStream(target, "wt")) {
                            if (out == null) throw new IOException("Cannot write to " + target);
                            out.write(file);
                        }
                        succeed(callback, null);
                    } catch (Exception failed) {
                        fail(callback, failed);
                    } finally {
                        clear(password);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                clear(password);
                callback.onError(error);
            }
        });
    }

    /** Reads the file's archive and manifest; nothing is decrypted or changed. */
    public void open(Uri source, Database.Callback<BackupFile.Opened> callback) {
        worker.execute(() -> {
            try {
                succeed(callback, BackupFile.open(readLimited(source)));
            } catch (Exception failed) {
                fail(callback, failed);
            }
        });
    }

    /**
     * Decrypts, reads and checks the data of an opened file.
     *
     * @param password ignored if the backup is not protected
     */
    public void load(BackupFile.Opened opened, char[] password, Database.Callback<BackupCleaner.Result> callback) {
        worker.execute(() -> {
            try {
                succeed(callback, opened.read(password));
            } catch (Exception failed) {
                fail(callback, failed);
            } finally {
                clear(password);
            }
        });
    }

    /** Replaces everything in the database with {@code data}, all or nothing. */
    public void replaceAll(BackupData data, Database.Callback<Void> callback) {
        database.write(db -> {
            TimetableStore.replaceAll(db, data);
            return null;
        }, callback);
    }

    private byte[] readLimited(Uri source) throws IOException, BackupException {
        try (var in = context.getContentResolver().openInputStream(source)) {
            if (in == null) throw new IOException("Cannot read " + source);
            var out = new ByteArrayOutputStream();
            var buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > BackupFile.MAX_FILE_BYTES) throw new BackupException(BackupException.Reason.TOO_LARGE);
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private <T> void succeed(Database.Callback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void fail(Database.Callback<T> callback, Exception error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private static void clear(char[] password) {
        if (password != null) Arrays.fill(password, '\0');
    }
}
