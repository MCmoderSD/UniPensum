package de.mcmodersd.unipensum.ui.widget;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.drawable.*;
import static de.mcmodersd.unipensum.R.string.*;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** A number with minus and plus buttons. Holding a button steps on, faster and faster, until the end of the range. */
public class UpStepper extends LinearLayout {

    public interface Formatter {
        CharSequence format(int value);
    }

    public interface OnValueChangedListener {
        void onValueChanged(int value);
    }

    private final TextView valueView;
    private final UpIconButton minus;
    private final UpIconButton plus;
    private int min = 1;
    private int max = 99;
    private int value = 1;
    private int step = 1;
    private Formatter formatter = String::valueOf;
    private OnValueChangedListener listener;

    public UpStepper(Context context) {
        this(context, null);
    }

    public UpStepper(Context context, AttributeSet attrs) {
        super(context, attrs);
        var dp = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(CENTER_VERTICAL);
        setPadding(Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp));
        var background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, surface));
        setBackground(background);

        minus = new UpIconButton(context);
        minus.setImageResource(ic_minus);
        minus.setContentDescription(context.getString(stepper_decrease));
        minus.setOnClickListener(v -> step(-1));
        minus.setOnHoldListener(() -> change(-1));
        // A step ticks and a refused step rejects, so the press itself stays quiet.
        minus.setClickHaptic(false);
        plus = new UpIconButton(context);
        plus.setImageResource(ic_add);
        plus.setContentDescription(context.getString(stepper_increase));
        plus.setOnClickListener(v -> step(1));
        plus.setOnHoldListener(() -> change(1));
        plus.setClickHaptic(false);

        valueView = new TextView(context);
        valueView.setGravity(CENTER);
        valueView.setTextSize(COMPLEX_UNIT_SP, 16);
        valueView.setTextColor(ContextCompat.getColor(context, text_primary));

        addView(minus);
        addView(valueView, new LayoutParams(0, WRAP_CONTENT, 1f));
        addView(plus);
        render();
    }

    /**
     * Names what the stepper sets for a screen reader, which would otherwise hear two buttons called "Decrease" and
     * "Increase" and a number. Show the same text above the stepper.
     */
    public void setLabel(CharSequence label) {
        minus.setContentDescription(getContext().getString(stepper_decrease_named, label));
        plus.setContentDescription(getContext().getString(stepper_increase_named, label));
    }

    public void setRange(int min, int max) {
        this.min = min;
        this.max = max;
        value = Math.max(min, Math.min(max, value));
        render();
    }

    /** How far one press moves the value; 1 by default. A value that is not a multiple stays until it is moved. */
    public void setStep(int step) {
        this.step = Math.max(1, step);
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
        render();
    }

    public int getValue() {
        return value;
    }

    /** Sets the value without notifying the listener. */
    public void setValue(int value) {
        this.value = Math.max(min, Math.min(max, value));
        render();
    }

    /**
     * Sets the value because of something the user did elsewhere, and says the new value to a screen reader, which
     * would otherwise not notice. Does not notify the listener.
     */
    public void changeTo(int value) {
        this.value = Math.max(min, Math.min(max, value));
        render(true);
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    private void step(int delta) {
        if (!change(delta)) Haptics.reject(this);
    }

    /** @return whether the value changed, which is not the case at the end of the range */
    private boolean change(int delta) {
        var next = Math.max(min, Math.min(max, value + delta * step));
        if (next == value) return false;
        value = next;
        render(true);
        Haptics.segmentTick(this);
        if (listener != null) listener.onValueChanged(value);
        return true;
    }

    private void render() {
        render(false);
    }

    /**
     * @param say whether a screen reader says the new value. A press is felt as a tick, which it cannot tell the
     *            result of; the value a screen opens with is not news, so the live region is off until a change
     *            that is
     */
    private void render(boolean say) {
        var text = formatter.format(value);
        // The same text again would be said again.
        if (!TextUtils.equals(valueView.getText(), text)) {
            // The live region stays on from the first change of the user: a screen reader looks at the view after it
            // has got the event, so one that is switched off again right away would not be heard.
            if (say) valueView.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
            valueView.setText(text);
        }
        minus.setDimmed(value <= min);
        plus.setDimmed(value >= max);
    }
}