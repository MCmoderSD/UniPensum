package de.mcmodersd.unipensum.ui.format;

import android.content.Context;
import android.text.format.DateFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.SessionType;

/** Locale- and 12/24h-aware formatting of times, dates and labels. */
public final class TimeFormat {

    private TimeFormat() { }

    public static Locale locale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    /** "08:00" on 24h devices, "8:00 AM" otherwise. */
    public static String time(Context context, int minutesSinceMidnight) {
        var time = LocalTime.of(minutesSinceMidnight / 60 % 24, minutesSinceMidnight % 60);
        var pattern = DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a";
        return DateTimeFormatter.ofPattern(pattern, locale(context)).format(time);
    }

    /** Full hour for the grid axis: "07:00" or "7 AM". */
    public static String hour(Context context, int hour) {
        var time = LocalTime.of(hour % 24, 0);
        var pattern = DateFormat.is24HourFormat(context) ? "HH:mm" : "h a";
        return DateTimeFormatter.ofPattern(pattern, locale(context)).format(time);
    }

    /** "Oct 5" or "5. Okt": the order and the punctuation of the app language, which needs no translated pattern. */
    public static String dateShort(Context context, LocalDate date) {
        var locale = locale(context);
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale).format(date);
    }

    /** "October 2026" for the title of a month, in the form of the app language. */
    public static String monthYear(Context context, YearMonth month) {
        var locale = locale(context);
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yyyyLLLL"), locale).format(month);
    }

    /** "Oct 5, 2026" or "05.10.2026", depending on the app language. */
    public static String dateMedium(Context context, LocalDate date) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale(context)).format(date);
    }

    public static String typeName(Context context, SessionType type) {
        return switch (type) {
            case LECTURE -> context.getString(R.string.type_lecture);
            case EXERCISE -> context.getString(R.string.type_exercise);
            case LAB -> context.getString(R.string.type_lab);
            case TUTORIAL -> context.getString(R.string.type_tutorial);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}