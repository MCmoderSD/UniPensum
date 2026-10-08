package de.mcmodersd.unipensum.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs all database work on one background thread. Every successful write bumps a revision
 * counter, and every {@link #observe} query re-runs when it changes, which is all the change
 * tracking a few hundred rows need.
 */
public final class Database {

    private static final String TAG = "Database";

    public interface Task<T> {
        T run(SQLiteDatabase db) throws Exception;
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Exception error);
    }

    private final DbHelper helper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "unipensum-db");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger revisionCounter = new AtomicInteger();
    private final MutableLiveData<Integer> revision = new MutableLiveData<>(0);
    private volatile Runnable writeListener;

    private Database(DbHelper helper) {
        this.helper = helper;
    }

    public static Database open(Context context) {
        return new Database(new DbHelper(context.getApplicationContext(), Schema.NAME));
    }

    /** Fresh database that vanishes with the process, for instrumented tests. */
    public static Database inMemory(Context context) {
        return new Database(new DbHelper(context.getApplicationContext(), null));
    }

    /** Runs a write transaction in the background and reports the result on the main thread. */
    public <T> void write(Task<T> task, Callback<T> callback) {
        executor.execute(() -> {
            T result;
            try {
                result = inTransaction(task);
            } catch (Exception error) {
                deliverError(callback, error);
                return;
            }
            revision.postValue(revisionCounter.incrementAndGet());
            var listener = writeListener;
            if (listener != null) listener.run();
            deliverSuccess(callback, result);
        });
    }

    /**
     * Called on the database thread after every write that went through, with the data already stored. For what
     * has to follow the data, such as the alarm of the next reminder.
     */
    public void setWriteListener(Runnable listener) {
        this.writeListener = listener;
    }

    /** One-off read in the background, reported on the main thread. */
    public <T> void read(Task<T> task, Callback<T> callback) {
        executor.execute(() -> {
            T result;
            try {
                result = task.run(helper.getReadableDatabase());
            } catch (Exception error) {
                deliverError(callback, error);
                return;
            }
            deliverSuccess(callback, result);
        });
    }

    /** Live query that is evaluated while observed and re-run after every write. */
    public <T> LiveData<T> observe(Task<T> query) {
        return new QueryLiveData<>(query);
    }

    private <T> T inTransaction(Task<T> task) throws Exception {
        var db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            var result = task.run(db);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private <T> void deliverSuccess(Callback<T> callback, T result) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void deliverError(Callback<T> callback, Exception error) {
        if (callback == null) {
            Log.e(TAG, "Database task failed", error);
        } else {
            mainHandler.post(() -> callback.onError(error));
        }
    }

    private final class QueryLiveData<T> extends LiveData<T> {

        private final Task<T> query;
        private final Observer<Integer> onRevision = ignored -> reload();

        QueryLiveData(Task<T> query) {
            this.query = query;
        }

        @Override
        protected void onActive() {
            // Delivers the current revision right away, which doubles as the initial load.
            revision.observeForever(onRevision);
        }

        @Override
        protected void onInactive() {
            revision.removeObserver(onRevision);
        }

        private void reload() {
            executor.execute(() -> {
                try {
                    postValue(query.run(helper.getReadableDatabase()));
                } catch (Exception error) {
                    Log.e(TAG, "Database query failed", error);
                }
            });
        }
    }
}
