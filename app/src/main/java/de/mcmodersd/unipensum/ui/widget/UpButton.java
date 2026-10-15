package de.mcmodersd.unipensum.ui.widget;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.CENTER;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.styleable.UpButton_upVariant;
import static de.mcmodersd.unipensum.ui.widget.UpButton.Variant.PRIMARY;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

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

        setGravity(CENTER);
        setMinHeight(Math.round(52 * dp));
        setPadding(Math.round(20 * dp), 0, Math.round(20 * dp), 0);
        setTextSize(COMPLEX_UNIT_SP, 16);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setClickable(true);
        setFocusable(true);
        setAllCaps(false);

        var variant = PRIMARY;
        if (attrs != null) {
            try (var array = context.obtainStyledAttributes(attrs, R.styleable.UpButton)) {
                variant = Variant.values()[array.getInt(UpButton_upVariant, 0)];
            }
        }
        setVariant(variant);
    }

    public void setVariant(Variant variant) {
        int fill;
        int text = switch (variant) {
            case SECONDARY -> {
                fill = surface;
                yield text_primary;
            }
            case DESTRUCTIVE -> {
                fill = surface;
                yield danger;
            }
            default -> {
                fill = accent;
                yield text_on_accent;
            }
        };
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