package de.mcmodersd.unipensum.ui.settings;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.AppSettings;
import de.mcmodersd.unipensum.data.AppSettings.ThemeMode;
import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.ui.Links;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.backup.BackupExportSheet;
import de.mcmodersd.unipensum.ui.backup.BackupImportSheet;
import de.mcmodersd.unipensum.ui.lecturer.LecturerSheet;
import de.mcmodersd.unipensum.ui.semester.SemesterSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSegmentedControl;
import de.mcmodersd.unipensum.ui.widget.UpStepper;

/** Theme, language, the visible hours of the grid and a way into the semester list. */
public class SettingsFragment extends Fragment {

    private static final int LANGUAGE_SYSTEM = 0;
    private static final int LANGUAGE_ENGLISH = 1;
    private static final int LANGUAGE_GERMAN = 2;
    /** A little longer than the segmented control's slide. */
    private static final long RECREATE_DELAY_MILLIS = 300;

    private final ActivityResultLauncher<String[]> pickBackup = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) BackupImportSheet.show(getParentFragmentManager(), uri);
            });

    private AppSettings settings;
    private UpStepper hoursStart;
    private UpStepper hoursEnd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = UniPensumApp.from(requireContext()).settings();
        ((TextView) view.findViewById(R.id.bar_title)).setText(R.string.settings_title);
        view.findViewById(R.id.back).setOnClickListener(v -> Navigator.of(this).pop());

        bindTheme(view.findViewById(R.id.theme_control));
        bindLanguage(view.findViewById(R.id.language_control));
        bindHours(view.findViewById(R.id.hours_start), view.findViewById(R.id.hours_end));
        bindLecturerNames(view.findViewById(R.id.lecturer_name_control));

        view.findViewById(R.id.manage_lecturers).setOnClickListener(v ->
                LecturerSheet.showManager(getParentFragmentManager()));
        view.findViewById(R.id.manage_semesters).setOnClickListener(v ->
                SemesterSheet.show(getParentFragmentManager()));
        view.findViewById(R.id.export_backup).setOnClickListener(v ->
                BackupExportSheet.show(getParentFragmentManager()));
        // The system's file dialog needs no permission. A backup has no registered file type, so any file
        // can be picked; what is not a backup is recognized and refused when it is opened.
        view.findViewById(R.id.import_backup).setOnClickListener(v -> pickBackup.launch(new String[]{"*/*"}));
        UpRow about = view.findViewById(R.id.about);
        about.setValue(versionName());
        // The project lives on GitHub; the link opens in the browser, which the app itself never needs.
        about.setOnClickListener(v -> {
            if (!Links.openWeb(requireContext(), getString(R.string.about_url))) Haptics.reject(v);
        });
    }

    private void bindTheme(UpSegmentedControl control) {
        control.setOptions(getString(R.string.theme_system), getString(R.string.theme_light),
                getString(R.string.theme_dark));
        control.setSelectedIndex(settings.themeMode().ordinal());
        // Changing the theme recreates the activity, which restores this screen from the back stack.
        control.setOnSelectionChangedListener(index ->
                afterSlide(control, () -> settings.setThemeMode(ThemeMode.values()[index])));
    }

    /**
     * Theme and language changes rebuild the whole screen at once. Waiting for the selection to finish
     * sliding keeps the switch visible instead of cutting it off.
     */
    private void afterSlide(View control, Runnable change) {
        control.postDelayed(() -> {
            if (isAdded()) change.run();
        }, RECREATE_DELAY_MILLIS);
    }

    private void bindLanguage(UpSegmentedControl control) {
        control.setOptions(getString(R.string.language_system), getString(R.string.language_english),
                getString(R.string.language_german));
        control.setSelectedIndex(currentLanguage());
        control.setOnSelectionChangedListener(index -> {
            String tags = index == LANGUAGE_ENGLISH ? "en" : (index == LANGUAGE_GERMAN ? "de" : "");
            afterSlide(control, () ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags)));
        });
    }

    private static int currentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty() || locales.get(0) == null) return LANGUAGE_SYSTEM;
        String language = locales.get(0).getLanguage();
        if ("en".equals(language)) return LANGUAGE_ENGLISH;
        if ("de".equals(language)) return LANGUAGE_GERMAN;
        return LANGUAGE_SYSTEM;
    }

    /** Nothing is rebuilt here: the grid and the sheets read the style from the settings when they draw. */
    private void bindLecturerNames(UpSegmentedControl control) {
        control.setOptions(getString(R.string.lecturer_name_last), getString(R.string.lecturer_name_full));
        NameStyle current = settings.lecturerNameStyle().getValue();
        control.setSelectedIndex(current == null ? NameStyle.LAST_NAME.ordinal() : current.ordinal());
        control.setOnSelectionChangedListener(index -> settings.setLecturerNameStyle(NameStyle.values()[index]));
    }

    private void bindHours(UpStepper start, UpStepper end) {
        hoursStart = start;
        hoursEnd = end;
        TimeWindow window = settings.timeWindow().getValue();
        UpStepper.Formatter hourLabel = value -> String.format(Locale.ROOT, "%02d:00", value);
        start.setFormatter(hourLabel);
        end.setFormatter(hourLabel);
        applyHourRanges(window);

        start.setOnValueChangedListener(value -> {
            TimeWindow current = settings.timeWindow().getValue();
            TimeWindow updated = new TimeWindow(value, current.endHour());
            settings.setTimeWindow(updated);
            applyHourRanges(updated);
        });
        end.setOnValueChangedListener(value -> {
            TimeWindow current = settings.timeWindow().getValue();
            TimeWindow updated = new TimeWindow(current.startHour(), value);
            settings.setTimeWindow(updated);
            applyHourRanges(updated);
        });
    }

    /** Each end can only move as far as the other leaves the minimum span. */
    private void applyHourRanges(TimeWindow window) {
        hoursStart.setRange(0, window.endHour() - TimeWindow.MIN_SPAN_HOURS);
        hoursStart.setValue(window.startHour());
        hoursEnd.setRange(window.startHour() + TimeWindow.MIN_SPAN_HOURS, 24);
        hoursEnd.setValue(window.endHour());
    }

    private String versionName() {
        try {
            String name = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            return getString(R.string.about_version, name);
        } catch (PackageManager.NameNotFoundException unknown) {
            return "";
        }
    }
}
