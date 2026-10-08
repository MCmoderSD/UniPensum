package de.mcmodersd.unipensum.ui.format;

import android.content.Context;

import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;

/** One-line descriptions of series and sessions for lists and the sheets. */
public final class SeriesFormat {

    private SeriesFormat() { }

    public static String weekdayShort(Context context, DayOfWeek weekday) {
        return weekday.getDisplayName(TextStyle.SHORT, TimeFormat.locale(context));
    }

    public static String weekdayFull(Context context, DayOfWeek weekday) {
        return weekday.getDisplayName(TextStyle.FULL, TimeFormat.locale(context));
    }

    public static String timeRange(Context context, SessionDetails details) {
        return TimeFormat.time(context, details.startMin()) + "–" + TimeFormat.time(context, details.endMin());
    }

    /** "Weekly" or "Every 3 weeks". */
    public static String repeat(Context context, int intervalWeeks) {
        return intervalWeeks == 1
                ? context.getString(R.string.repeat_weekly)
                : context.getResources().getQuantityString(R.plurals.repeat_every_n_weeks, intervalWeeks, intervalWeeks);
    }

    /**
     * Where the session takes place: "Online", the room, or the room with "| Hybrid" behind it
     * ("A2 | Hybrid", just "Hybrid" without a room). Empty if there is no room to show.
     */
    public static String place(Context context, SessionDetails details) {
        if (details.mode() == Mode.ONLINE) return context.getString(R.string.mode_online);
        var room = details.room() == null ? "" : details.room();
        if (!details.hybrid()) return room;
        var hybrid = context.getString(R.string.mode_hybrid);
        return room.isEmpty() ? hybrid : room + " | " + hybrid;
    }

    /** "Lecture · Mon · 08:00–10:30". */
    public static String title(Context context, Series series) {
        return TimeFormat.typeName(context, series.details().type())
                + " · " + weekdayShort(context, series.schedule().weekday())
                + " · " + timeRange(context, series.details());
    }

    /**
     * "Weekly · A1 · Weber", skipping what is empty.
     *
     * @param lecturerName the series' lecturer as it should be shown, {@code null} for none
     */
    public static String subtitle(Context context, Series series, @Nullable String lecturerName) {
        var parts = new ArrayList<String>();
        parts.add(repeat(context, series.schedule().intervalWeeks()));
        var place = place(context, series.details());
        if (!place.isEmpty()) parts.add(place);
        if (lecturerName != null) parts.add(lecturerName);
        return String.join(" · ", parts);
    }
}