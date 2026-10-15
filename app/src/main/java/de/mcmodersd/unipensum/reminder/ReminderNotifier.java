package de.mcmodersd.unipensum.reminder;

import static android.app.Notification.CATEGORY_EVENT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.PendingIntent.*;
import static android.content.Intent.*;
import static de.mcmodersd.unipensum.R.drawable.ic_notification;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.ui.MainActivity.EXTRA_SESSION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.time.ZoneId;
import java.util.ArrayList;

import de.mcmodersd.unipensum.data.ReminderView;
import de.mcmodersd.unipensum.domain.logic.Reminders;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;
import de.mcmodersd.unipensum.ui.MainActivity;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * Shows a reminder as a notification of its own kind, like the reminder of a calendar app: it comes up as a
 * banner, stays until it is swiped away or the event is over, and has buttons for the meeting and for Moodle.
 * A tap opens the event in the app.
 */
public final class ReminderNotifier {

    static final String CHANNEL_ID = "reminders";
    private static final String TAG = "reminder";

    private ReminderNotifier() { }

    /** Idempotent. The user can change sound and priority of the channel in the system settings. */
    public static void createChannel(Context context) {
        var channel = new NotificationChannel(CHANNEL_ID, context.getString(reminder_channel_name), IMPORTANCE_HIGH);
        channel.setDescription(context.getString(reminder_channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    static void show(Context context, @Nullable ReminderView view, Reminders.Due due) {
        if (view == null) return;
        createChannel(context);
        var details = view.session().details();

        // The texts follow the language of the app, which Android also applies when only a receiver runs.
        var parts = new ArrayList<String>();
        parts.add(TimeFormat.typeName(context, details.type()));
        parts.add(SeriesFormat.timeRange(context, details));
        var place = SeriesFormat.place(context, details);
        if (!place.isEmpty()) parts.add(place);

        var startMillis = due.start().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        var endMillis = due.end().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        var id = idOf(view.session().id());

        var builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(ic_notification)
                .setContentTitle(view.courseName())
                .setContentText(String.join(" · ", parts))
                .setCategory(CATEGORY_EVENT)
                .setWhen(startMillis)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setTimeoutAfter(Math.max(1, endMillis - System.currentTimeMillis()))
                .setContentIntent(openEvent(context, view.session().id(), id));

        if (details.link() != null) {
            builder.addAction(action(context, id, 1, context.getString(action_open_meeting), details.link()));
        }
        if (view.moodleLink() != null) {
            builder.addAction(action(context, id, 2, context.getString(action_open_moodle), view.moodleLink()));
        }
        context.getSystemService(NotificationManager.class).notify(TAG, id, builder.build());
    }

    /** A session id as the id of its notification; ids are small, this only makes sure they fit. */
    private static int idOf(long sessionId) {
        return (int) (sessionId % Integer.MAX_VALUE);
    }

    private static PendingIntent openEvent(Context context, long sessionId, int id) {
        var intent = new Intent(context, MainActivity.class)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, id, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /** A button that opens a web link. The link was checked when it was saved, and is checked again here. */
    private static Notification.Action action(Context context, int id, int slot, String title, String link) {
        String url;
        try {
            url = TextSanitizer.webLink(link);
        } catch (IllegalArgumentException notAWebLink) {
            url = null;
        }
        var intent = new Intent(ACTION_VIEW, Uri.parse(url == null ? "about:blank" : url))
                .addFlags(FLAG_ACTIVITY_NEW_TASK);
        var pending = PendingIntent.getActivity(context, id * 4 + slot, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
        return new Notification.Action.Builder(null, title, pending).build();
    }
}