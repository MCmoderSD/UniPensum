package de.mcmodersd.unipensum.ui.format;

import static de.mcmodersd.unipensum.R.string.*;

import android.content.Context;

/** How long before an event it reminds: "At the start", "30 min before", "1 h 30 min before". */
public final class ReminderFormat {

    private ReminderFormat() { }

    public static String text(Context context, int minutes) {
        if (minutes <= 0) return context.getString(reminder_at_start);
        if (minutes < 60) return context.getString(reminder_minutes_before, minutes);
        var hours = minutes / 60;
        var rest = minutes % 60;
        return rest == 0
                ? context.getString(reminder_hours_before, hours)
                : context.getString(reminder_hours_minutes_before, hours, rest);
    }
}