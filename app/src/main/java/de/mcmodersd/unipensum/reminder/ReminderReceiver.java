package de.mcmodersd.unipensum.reminder;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Wakes the app for the next reminder, and after everything that clears or moves the alarm: a restart of the
 * phone, a new clock or time zone, an update of the app, a change of the exact alarm permission. All of them
 * lead to the same run, see {@link ReminderScheduler#update}.
 */
public final class ReminderReceiver extends BroadcastReceiver {

    /** Sent by the alarm itself. */
    public static final String ACTION_ALARM = "de.mcmodersd.unipensum.action.REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isKnown(intent.getAction())) return;
        // The database read and the notification take a moment; the system waits for them.
        PendingResult result = goAsync();
        ReminderScheduler.update(context, true, result::finish);
    }

    private static boolean isKnown(String action) {
        if (action == null) return false;
        switch (action) {
            case ACTION_ALARM:
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED:
                return true;
            default:
                return false;
        }
    }
}
