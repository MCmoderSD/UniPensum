package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.R;

/** A number with minus and plus buttons. */
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
    private Formatter formatter = String::valueOf;
    private OnValueChangedListener listener;

    public UpStepper(Context context) {
        this(context, null);
    }

    public UpStepper(Context context, AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, R.color.surface));
        setBackground(background);

        minus = new UpIconButton(context);
        minus.setImageResource(R.drawable.ic_minus);
        minus.setContentDescription(context.getString(R.string.stepper_decrease));
        minus.setOnClickListener(v -> step(-1));
        plus = new UpIconButton(context);
        plus.setImageResource(R.drawable.ic_add);
        plus.setContentDescription(context.getString(R.string.stepper_increase));
        plus.setOnClickListener(v -> step(1));

        valueView = new TextView(context);
        valueView.setGravity(Gravity.CENTER);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        valueView.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

        addView(minus);
        addView(valueView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        addView(plus);
        render();
    }

    public void setRange(int min, int max) {
        this.min = min;
        this.max = max;
        value = Math.max(min, Math.min(max, value));
        render();
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

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    private void step(int delta) {
        int next = Math.max(min, Math.min(max, value + delta));
        if (next == value) {
            Haptics.reject(this);
            return;
        }
        value = next;
        render();
        Haptics.segmentTick(this);
        if (listener != null) listener.onValueChanged(value);
    }

    private void render() {
        valueView.setText(formatter.format(value));
        minus.setDimmed(value <= min);
        plus.setDimmed(value >= max);
    }
}
