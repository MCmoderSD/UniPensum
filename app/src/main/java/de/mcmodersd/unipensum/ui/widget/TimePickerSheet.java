package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import de.mcmodersd.unipensum.R;

/**
 * Picks a time of day. The wheels move in 5-minute steps; the keyboard button in the header switches
 * to two number fields for typing any minute. Delivers {@link #RESULT_MINUTES} under the request key.
 */
public final class TimePickerSheet extends UpSheet {

    public static final String RESULT_MINUTES = "minutes";

    private static final int STEP_MINUTES = 5;
    private static final String ARG_KEY = "key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MINUTES = "minutes";
    private static final String STATE_MINUTES = "state_minutes";
    private static final String STATE_TYPING = "state_typing";

    private final ArrayList<Integer> minuteValues = new ArrayList<>();
    private WheelPicker hourWheel;
    private WheelPicker minuteWheel;
    private View wheelsLayer;
    private View typingLayer;
    private AppCompatEditText hourInput;
    private AppCompatEditText minuteInput;
    private boolean typing;

    public static void show(FragmentManager manager, String requestKey, CharSequence title, int minutesSinceMidnight) {
        var args = new Bundle();
        args.putString(ARG_KEY, requestKey);
        args.putCharSequence(ARG_TITLE, title);
        args.putInt(ARG_MINUTES, minutesSinceMidnight);
        var sheet = new TimePickerSheet();
        sheet.setArguments(args);
        sheet.show(manager, "time:" + requestKey);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return requireArguments().getCharSequence(ARG_TITLE);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var initial = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_MINUTES)
                : requireArguments().getInt(ARG_MINUTES);
        typing = savedInstanceState != null && savedInstanceState.getBoolean(STATE_TYPING);

        // Both layers share one frame of fixed height, so switching between them does not move the sheet.
        var stack = new FrameLayout(context);
        wheelsLayer = buildWheels(context, dp);
        typingLayer = buildTypingFields(context, dp);
        var height = Math.round(5 * 44 * dp);
        stack.addView(wheelsLayer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        stack.addView(typingLayer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        showTime(initial / 60 % 24, initial % 60);

        var done = new UpButton(context);
        done.setText(R.string.action_done);
        done.setOnClickListener(this::finish);

        var column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(
                stack, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        var doneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        doneParams.topMargin = Math.round(12 * dp);
        column.addView(done, doneParams);
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyMode(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        var time = currentTime();
        outState.putInt(STATE_MINUTES, time == null ? requireArguments().getInt(ARG_MINUTES) : time[0] * 60 + time[1]);
        outState.putBoolean(STATE_TYPING, typing);
    }

    // --- wheels ---

    private View buildWheels(Context context, float dp) {
        var hours = new String[24];
        for (var h = 0; h < 24; h++) hours[h] = twoDigits(h);
        hourWheel = new WheelPicker(context);
        hourWheel.setLabels(hours);
        minuteWheel = new WheelPicker(context);

        var colon = colon(context, 22);
        var wheels = new LinearLayout(context);
        wheels.setGravity(Gravity.CENTER);
        wheels.addView(hourWheel, new LinearLayout.LayoutParams(Math.round(88 * dp), ViewGroup.LayoutParams.WRAP_CONTENT));
        wheels.addView(colon, new LinearLayout.LayoutParams(Math.round(20 * dp), ViewGroup.LayoutParams.WRAP_CONTENT));
        wheels.addView(minuteWheel, new LinearLayout.LayoutParams(Math.round(88 * dp), ViewGroup.LayoutParams.WRAP_CONTENT));

        // The band behind the wheels marks the selected row.
        var band = new View(context);
        band.setBackground(rounded(context, 12 * dp));
        var layer = new FrameLayout(context);
        layer.addView(
                band, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.round(44 * dp), Gravity.CENTER_VERTICAL
                )
        );
        layer.addView(
                wheels, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
                )
        );
        return layer;
    }

    /** Fills the minute wheel with the 5-minute steps plus {@code minute} itself if it lies in between. */
    private void setMinuteWheel(int minute) {
        minuteValues.clear();
        for (var m = 0; m < 60; m += STEP_MINUTES) minuteValues.add(m);
        if (!minuteValues.contains(minute)) {
            var index = 0;
            while (index < minuteValues.size() && minuteValues.get(index) < minute) index++;
            minuteValues.add(index, minute);
        }
        var labels = new String[minuteValues.size()];
        for (var i = 0; i < labels.length; i++) labels[i] = twoDigits(minuteValues.get(i));
        minuteWheel.setLabels(labels);
        minuteWheel.setSelectedIndex(minuteValues.indexOf(minute));
    }

    // --- typing ---

    private View buildTypingFields(Context context, float dp) {
        hourInput = numberField(context, dp, getString(R.string.time_hour));
        minuteInput = numberField(context, dp, getString(R.string.time_minute));
        hourInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        minuteInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        minuteInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            finish(v);
            return true;
        });
        // After two digits, or one that can only be a full hour, move on to the minutes.
        hourInput.addTextChangedListener(
                new AfterChange(text -> {
                    var complete = text.length() == 2 || (text.length() == 1 && text.charAt(0) > '2');
                    if (complete && hourInput.hasFocus()) minuteInput.requestFocus();
                })
        );

        var row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER);
        row.addView(hourInput, new LinearLayout.LayoutParams(Math.round(96 * dp), Math.round(68 * dp)));
        row.addView(colon(context, 28), new LinearLayout.LayoutParams(Math.round(28 * dp), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(minuteInput, new LinearLayout.LayoutParams(Math.round(96 * dp), Math.round(68 * dp)));
        return row;
    }

    private AppCompatEditText numberField(Context context, float dp, String description) {
        var field = new AppCompatEditText(context);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        field.setGravity(Gravity.CENTER);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        field.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        field.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        field.setSelectAllOnFocus(true);
        field.setPadding(0, 0, 0, 0);
        field.setBackground(rounded(context, 14 * dp));
        field.setContentDescription(description);
        field.addTextChangedListener(new AfterChange(text -> markValid(field, true)));
        return field;
    }

    private void markValid(AppCompatEditText field, boolean valid) {
        var background = (GradientDrawable) field.getBackground();
        var dp = getResources().getDisplayMetrics().density;
        background.setStroke(valid ? 0 : Math.round(1.5f * dp), ContextCompat.getColor(requireContext(), R.color.danger));
    }

    // --- mode and result ---

    /** Puts the same time into the wheels and the typing fields. */
    private void showTime(int hour, int minute) {
        hourWheel.setSelectedIndex(hour);
        setMinuteWheel(minute);
        hourInput.setText(twoDigits(hour));
        minuteInput.setText(twoDigits(minute));
    }

    /**
     * @return hour and minute of the active mode, or {@code null} if the typed values are not a time
     */
    @Nullable
    private int[] currentTime() {
        if (!typing) {
            return new int[]{hourWheel.getSelectedIndex(), minuteValues.get(minuteWheel.getSelectedIndex())};
        }
        var hour = parse(hourInput, 23);
        var minute = parse(minuteInput, 59);
        markValid(hourInput, hour != null);
        markValid(minuteInput, minute != null);
        return hour == null || minute == null ? null : new int[]{hour, minute};
    }

    @Nullable
    private static Integer parse(AppCompatEditText field, int max) {
        var text = field.getText() == null ? "" : field.getText().toString().trim();
        // Digits only: parseInt would also take "+5" or "-0", which a paste can bring in.
        if (!text.matches("[0-9]{1,2}")) return null;
        try {
            var value = Integer.parseInt(text);
            return value >= 0 && value <= max ? value : null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private void toggleMode() {
        var time = currentTime();
        if (time == null) {
            // Typed nonsense: stay in typing mode and show which field is wrong.
            Haptics.reject(typingLayer);
            return;
        }
        typing = !typing;
        showTime(time[0], time[1]);
        applyMode(true);
    }

    private void applyMode(boolean byUser) {
        wheelsLayer.setVisibility(typing ? View.INVISIBLE : View.VISIBLE);
        typingLayer.setVisibility(typing ? View.VISIBLE : View.INVISIBLE);
        setHeaderAction(
                typing ? R.drawable.ic_clock : R.drawable.ic_keyboard,
                getString(typing ? R.string.time_use_wheels : R.string.time_type), v -> toggleMode()
        );

        var window = window();
        if (window == null) return;
        if (typing) {
            hourInput.requestFocus();
            hourInput.selectAll();
            hourInput.post(() -> {
                // The sheet may be gone by now.
                var current = window();
                if (current != null) WindowCompat.getInsetsController(current, hourInput).show(WindowInsetsCompat.Type.ime());
            });
        } else if (byUser) {
            hourInput.clearFocus();
            minuteInput.clearFocus();
            WindowCompat.getInsetsController(window, hourInput).hide(WindowInsetsCompat.Type.ime());
        }
    }

    /** @return the window of the sheet, or {@code null} while there is none */
    @Nullable
    private Window window() {
        var dialog = getDialog();
        return dialog == null ? null : dialog.getWindow();
    }

    private void finish(View source) {
        var time = currentTime();
        if (time == null) {
            Haptics.reject(source);
            return;
        }
        var result = new Bundle();
        result.putInt(RESULT_MINUTES, time[0] * 60 + time[1]);
        getParentFragmentManager().setFragmentResult(Objects.requireNonNull(requireArguments().getString(ARG_KEY)), result);
        dismiss();
    }

    // --- small helpers ---

    private static String twoDigits(int value) {
        return String.format(Locale.ROOT, "%02d", value);
    }

    private static TextView colon(Context context, float sp) {
        var colon = new TextView(context);
        colon.setText(":");
        colon.setGravity(Gravity.CENTER);
        colon.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        colon.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        return colon;
    }

    private static GradientDrawable rounded(Context context, float radius) {
        var drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setColor(ContextCompat.getColor(context, R.color.surface));
        return drawable;
    }

    private interface TextChange {
        void onChanged(String text);
    }

    private record AfterChange(TextChange action) implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            action.onChanged(s.toString());
        }
    }
}