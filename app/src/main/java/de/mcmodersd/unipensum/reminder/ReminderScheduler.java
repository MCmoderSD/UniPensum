package de.mcmodersd.unipensum.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.ReminderView;
import de.mcmodersd.unipensum.data.TimetableStore;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.logic.Reminders;
import de.mcmodersd.unipensum.domain.model.Session;

/**
 * Keeps the reminders in step with the data. There is always exactly one alarm: the one for the next reminder.
 * Every run does the same, whatever caused it: show the reminders that became due since the last run, then set
 * the alarm for the next one (or none). An alarm for every session would have to follow every edit of every
 * session; one alarm that is recomputed from the data cannot get out of step with it.
 */
public final class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";
    private static final String PREFS = "reminders";
    /** When the last run looked at the data, in epoch milliseconds. */
    private static final String KEY_LAST_RUN = "last_run";

    private ReminderScheduler() {
    }

    /**
     * Runs on the database thread and finishes on the main thread.
     *
     * @param showDue whether the reminders that became due since the last run are shown. Runs after a change of
     *                the data only plan: a session that was just created or moved to a time that is already in
     *                the past must not remind of itself.
     * @param done    called when the run is over, also if it failed; may be {@code null}
     */
    public static void update(Context context, boolean showDue, @Nullable Runnable done) {
        var app = context.getApplicationContext();
        var zone = ZoneId.systemDefault();
        var nowMillis = System.currentTimeMillis();
        var now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone);

        // Yesterday as well: a reminder for an early session falls on the day before it.
        UniPensumApp.from(app).database().read(
                db -> TimetableStore.loadReminders(db, now.toLocalDate().minusDays(1)),
                new Database.Callback<List<ReminderView>>() {
                    @Override
                    public void onSuccess(List<ReminderView> views) {
                        try {
                            plan(app, views, showDue, nowMillis, now, zone);
                        } finally {
                            if (done != null) done.run();
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Could not read the reminders", error);
                        if (done != null) done.run();
                    }
                });
    }

    private static void plan(Context context, List<ReminderView> views, boolean showDue, long nowMillis,
                             LocalDateTime now, ZoneId zone) {
        var state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        var lastRun = state.getLong(KEY_LAST_RUN, 0);
        // The first run has no past: nothing that was due before the app was installed is shown.
        var after = lastRun == 0 ? now : LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRun), zone);
        var enabled = UniPensumApp.from(context).settings().remindersEnabled();

        var sessions = new ArrayList<Session>();
        var byId = new HashMap<Long, ReminderView>();
        for (var view : views) {
            sessions.add(view.session());
            byId.put(view.session().id(), view);
        }
        if (enabled && showDue) {
            for (var due : Reminders.due(sessions, after, now)) {
                ReminderNotifier.show(context, byId.get(due.session().id()), due);
            }
        }
        state.edit().putLong(KEY_LAST_RUN, nowMillis).apply();

        Optional<LocalDateTime> next = enabled ? Reminders.next(sessions, now) : Optional.empty();
        setAlarm(context, next.map(time -> time.atZone(zone).toInstant().toEpochMilli()).orElse(null));
    }

    /** @param triggerMillis when the alarm goes off, {@code null} to have none */
    private static void setAlarm(Context context, @Nullable Long triggerMillis) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        var pending = alarmIntent(context);
        if (triggerMillis == null) {
            alarms.cancel(pending);
        } else if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending);
        } else {
            // Without the permission to be exact, the system may be a few minutes late, but still keeps the alarm.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending);
        }
    }

    private static PendingIntent alarmIntent(Context context) {
        var intent = new Intent(context, ReminderReceiver.class).setAction(ReminderReceiver.ACTION_ALARM);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
