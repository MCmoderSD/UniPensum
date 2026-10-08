package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.R;

/** The app's button. Primary is filled, secondary is a quiet card, destructive is a card in the danger color. */
public class UpButton extends AppCompatTextView {

    public enum Variant {PRIMARY, SECONDARY, DESTRUCTIVE}

    public UpButton(Context context) {
        this(context, null);
    }

    public UpButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        var dp = getResources().getDisplayMetrics().density;

        setGravity(Gravity.CENTER);
        setMinHeight(Math.round(52 * dp));
        setPadding(Math.round(20 * dp), 0, Math.round(20 * dp), 0);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setClickable(true);
        setFocusable(true);
        setAllCaps(false);

        var variant = Variant.PRIMARY;
        if (attrs != null) {
            var array = context.obtainStyledAttributes(attrs, R.styleable.UpButton);
            variant = Variant.values()[array.getInt(R.styleable.UpButton_upVariant, 0)];
            array.recycle();
        }
        setVariant(variant);
    }

    public void setVariant(Variant variant) {
        int fill;
        int text;
        switch (variant) {
            case SECONDARY:
                fill = R.color.surface;
                text = R.color.text_primary;
                break;
            case DESTRUCTIVE:
                fill = R.color.surface;
                text = R.color.danger;
                break;
            case PRIMARY:
            default:
                fill = R.color.accent;
                text = R.color.text_on_accent;
                break;
        }
        var background = new GradientDrawable();
        background.setCornerRadius(14 * getResources().getDisplayMetrics().density);
        background.setColor(ContextCompat.getColor(getContext(), fill));
        setBackground(background);
        setTextColor(ContextCompat.getColor(getContext(), text));
    }

    @Override
    public boolean performClick() {
        Haptics.tap(this);
        return super.performClick();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        updateAlpha();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateAlpha();
    }

    private void updateAlpha() {
        setAlpha(!isEnabled() ? 0.4f : (isPressed() ? 0.7f : 1f));
    }
}
