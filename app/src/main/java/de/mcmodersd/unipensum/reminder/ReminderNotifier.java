package de.mcmodersd.unipensum.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.LocaleList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.ReminderView;
import de.mcmodersd.unipensum.domain.logic.Reminders;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
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

    private ReminderNotifier() {
    }

    /** Idempotent. The user can change sound and priority of the channel in the system settings. */
    public static void createChannel(Context context) {
        Context localized = localized(context);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                localized.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(localized.getString(R.string.reminder_channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    static void show(Context context, @Nullable ReminderView view, Reminders.Due due) {
        if (view == null) return;
        Context text = localized(context);
        createChannel(context);
        SessionDetails details = view.session().details();

        List<String> parts = new ArrayList<>();
        parts.add(TimeFormat.typeName(text, details.type()));
        parts.add(SeriesFormat.timeRange(text, details));
        String place = SeriesFormat.place(text, details);
        if (!place.isEmpty()) parts.add(place);

        long startMillis = due.start().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = due.end().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        int id = idOf(view.session().id());

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(view.courseName())
                .setContentText(String.join(" · ", parts))
                .setCategory(Notification.CATEGORY_EVENT)
                .setWhen(startMillis)
                .setShowWhen(true)
                .setAutoCancel(true)
                // Over, it is no use any more: it goes away by itself.
                .setTimeoutAfter(Math.max(1, endMillis - System.currentTimeMillis()))
                .setContentIntent(openEvent(context, view.session().id(), id));

        if (details.link() != null) {
            builder.addAction(action(context, id, 1, text.getString(R.string.action_open_meeting), details.link()));
        }
        if (view.moodleLink() != null) {
            builder.addAction(action(context, id, 2, text.getString(R.string.action_open_moodle), view.moodleLink()));
        }
        context.getSystemService(NotificationManager.class).notify(TAG, id, builder.build());
    }

    /** A session id as the id of its notification; ids are small, this only makes sure they fit. */
    private static int idOf(long sessionId) {
        return (int) (sessionId % Integer.MAX_VALUE);
    }

    private static PendingIntent openEvent(Context context, long sessionId, int id) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_SESSION_ID, sessionId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** A button that opens a web link. The link was checked when it was saved, and is checked again here. */
    private static Notification.Action action(Context context, int id, int slot, String title, String link) {
        String url;
        try {
            url = TextSanitizer.webLink(link);
        } catch (IllegalArgumentException notAWebLink) {
            url = null;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url == null ? "about:blank" : url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, id * 4 + slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(null, title, pending).build();
    }

    /**
     * The language chosen in the app. On Android 13 and newer the whole app process already follows it, before
     * that only the screens do, so the text of a reminder is looked up in a context of that language.
     */
    private static Context localized(Context context) {
        LocaleListCompat chosen = AppCompatDelegate.getApplicationLocales();
        if (chosen.isEmpty()) return context;
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()));
        return context.createConfigurationContext(configuration);
    }
}
