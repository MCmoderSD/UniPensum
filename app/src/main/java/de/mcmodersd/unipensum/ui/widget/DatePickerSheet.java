package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.time.LocalDate;
import java.util.Objects;

/** Picks a date from a month calendar. Delivers {@link #RESULT_EPOCH_DAY} under the request key and closes. */
public final class DatePickerSheet extends UpSheet {

    public static final String RESULT_EPOCH_DAY = "epochDay";

    private static final String ARG_KEY = "key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_MIN = "min";
    private static final String ARG_MAX = "max";
    private static final String ARG_WEEKDAYS_ONLY = "weekdaysOnly";

    /**
     * @param min          earliest selectable date, or {@code null} for no limit
     * @param max          latest selectable date, or {@code null} for no limit
     * @param weekdaysOnly hide Saturday and Sunday
     */
    public static void show(FragmentManager manager, String requestKey, CharSequence title, LocalDate selected,
                            @Nullable LocalDate min, @Nullable LocalDate max, boolean weekdaysOnly) {
        var args = new Bundle();
        args.putString(ARG_KEY, requestKey);
        args.putCharSequence(ARG_TITLE, title);
        args.putLong(ARG_SELECTED, selected.toEpochDay());
        args.putLong(ARG_MIN, min == null ? LocalDate.of(2000, 1, 1).toEpochDay() : min.toEpochDay());
        args.putLong(ARG_MAX, max == null ? LocalDate.of(2100, 12, 31).toEpochDay() : max.toEpochDay());
        args.putBoolean(ARG_WEEKDAYS_ONLY, weekdaysOnly);
        var sheet = new DatePickerSheet();
        sheet.setArguments(args);
        sheet.show(manager, "date:" + requestKey);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return requireArguments().getCharSequence(ARG_TITLE);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        var args = requireArguments();
        var calendar = new MonthCalendarView(requireContext());
        calendar.setWeekdaysOnly(args.getBoolean(ARG_WEEKDAYS_ONLY));
        calendar.setRange(LocalDate.ofEpochDay(args.getLong(ARG_MIN)), LocalDate.ofEpochDay(args.getLong(ARG_MAX)));
        calendar.setSelected(LocalDate.ofEpochDay(args.getLong(ARG_SELECTED)));
        calendar.setOnDateSelectedListener(date -> {
            var result = new Bundle();
            result.putLong(RESULT_EPOCH_DAY, date.toEpochDay());
            getParentFragmentManager().setFragmentResult(Objects.requireNonNull(args.getString(ARG_KEY)), result);
            dismiss();
        });
        return calendar;
    }
}