package de.mcmodersd.unipensum.ui.format;

import android.content.Context;

import de.mcmodersd.unipensum.R;

/** How long before an event it reminds: "At the start", "30 min before", "1 h 30 min before". */
public final class ReminderFormat {

    private ReminderFormat() { }

    public static String text(Context context, int minutes) {
        if (minutes <= 0) return context.getString(R.string.reminder_at_start);
        if (minutes < 60) return context.getString(R.string.reminder_minutes_before, minutes);
        var hours = minutes / 60;
        var rest = minutes % 60;
        return rest == 0
                ? context.getString(R.string.reminder_hours_before, hours)
                : context.getString(R.string.reminder_hours_minutes_before, hours, rest);
    }
}
