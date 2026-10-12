package de.mcmodersd.unipensum.data;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Objects;

import de.mcmodersd.unipensum.domain.model.NameStyle;

/**
 * User preferences. The language is not stored here: it is set in the Android settings.
 */
public final class AppSettings {

    /** The choice is handed to the system (see {@link #applyTheme()}), so these are the modes of {@link UiModeManager}. */
    public enum ThemeMode {
        SYSTEM(UiModeManager.MODE_NIGHT_AUTO),
        LIGHT(UiModeManager.MODE_NIGHT_NO),
        DARK(UiModeManager.MODE_NIGHT_YES);

        final int nightMode;

        ThemeMode(int nightMode) {
            this.nightMode = nightMode;
        }
    }

    /** Visible hours of the week grid, {@code endHour} exclusive, at least {@link #MIN_SPAN_HOURS} long. */
    public record TimeWindow(int startHour, int endHour) {

        public static final int MIN_SPAN_HOURS = 4;

        public TimeWindow {
            if (startHour < 0 || endHour > 24 || endHour - startHour < MIN_SPAN_HOURS) {
                throw new IllegalArgumentException("Invalid time window: " + startHour + "-" + endHour);
            }
        }

        public static final TimeWindow DEFAULT = new TimeWindow(7, 22);
    }

    private static final String PREFS = "settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_WINDOW_START = "window_start";
    private static final String KEY_WINDOW_END = "window_end";
    private static final String KEY_LECTURER_NAME = "lecturer_name";
    private static final String KEY_REMINDERS = "reminders";
    private static final String KEY_NOTIFICATIONS_ASKED = "notifications_asked";

    private final SharedPreferences prefs;
    private final UiModeManager uiModeManager;
    private final MutableLiveData<TimeWindow> timeWindow = new MutableLiveData<>();
    private final MutableLiveData<NameStyle> lecturerNameStyle = new MutableLiveData<>();

    public AppSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        uiModeManager = context.getSystemService(UiModeManager.class);
        timeWindow.setValue(readTimeWindow());
        lecturerNameStyle.setValue(readLecturerNameStyle());
    }

    /** How lecturers are named in the grid, the lists and the sheets; the last name only by default. */
    public LiveData<NameStyle> lecturerNameStyle() {
        return lecturerNameStyle;
    }

    /** Must be called on the main thread. */
    public void setLecturerNameStyle(NameStyle style) {
        prefs.edit().putString(KEY_LECTURER_NAME, style.name()).apply();
        lecturerNameStyle.setValue(style);
    }

    private NameStyle readLecturerNameStyle() {
        try {
            return NameStyle.valueOf(prefs.getString(KEY_LECTURER_NAME, NameStyle.LAST_NAME.name()));
        } catch (IllegalArgumentException unknown) {
            return NameStyle.LAST_NAME;
        }
    }

    /** Whether the app reminds of events at all; every event still has its own time. On by default. */
    public boolean remindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS, true);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply();
    }

    /** Whether the user has been asked to allow notifications. The app asks once and then leaves it be. */
    public boolean notificationsAsked() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false);
    }

    public void setNotificationsAsked() {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply();
    }

    public ThemeMode themeMode() {
        try {
            return ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name()));
        } catch (IllegalArgumentException unknown) {
            return ThemeMode.SYSTEM;
        }
    }

    public void setThemeMode(ThemeMode mode) {
        prefs.edit().putString(KEY_THEME, mode.name()).apply();
        applyTheme();
    }

    /**
     * Tells the system which theme the app wants, instead of leaving it to the activity. The system keeps the choice
     * for the app and applies it to every screen, also to the splash screen, which it draws before any code of the app
     * runs and which would otherwise follow the theme of the phone. Call once at startup.
     */
    public void applyTheme() {
        uiModeManager.setApplicationNightMode(themeMode().nightMode);
    }

    public LiveData<TimeWindow> timeWindow() {
        return timeWindow;
    }

    /** The window as it is now. It is set in the constructor, so unlike the value of the LiveData it is never null. */
    public TimeWindow currentTimeWindow() {
        return Objects.requireNonNull(timeWindow.getValue());
    }

    /** Must be called on the main thread. */
    public void setTimeWindow(TimeWindow window) {
        prefs.edit()
                .putInt(KEY_WINDOW_START, window.startHour())
                .putInt(KEY_WINDOW_END, window.endHour())
                .apply();
        timeWindow.setValue(window);
    }

    private TimeWindow readTimeWindow() {
        try {
            return new TimeWindow(
                    prefs.getInt(KEY_WINDOW_START, TimeWindow.DEFAULT.startHour()),
                    prefs.getInt(KEY_WINDOW_END, TimeWindow.DEFAULT.endHour())
            );
        } catch (IllegalArgumentException invalid) {
            return TimeWindow.DEFAULT;
        }
    }
}