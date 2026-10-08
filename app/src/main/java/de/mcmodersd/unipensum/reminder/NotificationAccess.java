package de.mcmodersd.unipensum.reminder;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

/** Whether the app may show notifications, and the way to the system settings where that is changed. */
public final class NotificationAccess {

    private NotificationAccess() { }

    /** False if the user turned the notifications of the app off, or denied them when asked (Android 13 and newer). */
    public static boolean allowed(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Opens the notification settings of the app. */
    public static void openSettings(Context context) {
        var intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}