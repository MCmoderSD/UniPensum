package de.mcmodersd.unipensum.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.function.BooleanSupplier;

import de.mcmodersd.unipensum.R;

/** A tappable icon with a 44dp touch target. Set a content description wherever it is used. */
public class UpIconButton extends AppCompatImageView {

    private static final long FIRST_INTERVAL_MS = 160;
    private static final long FASTEST_INTERVAL_MS = 50;
    private static final long SPEED_UP_MS = 12;

    private final Runnable repeater = new Runnable() {
        @Override
        public void run() {
            if (holdAction == null || !holdAction.getAsBoolean()) return;
            held = true;
            postDelayed(this, interval);
            interval = Math.max(FASTEST_INTERVAL_MS, interval - SPEED_UP_MS);
        }
    };
    private BooleanSupplier holdAction;
    /** True once the current touch has run the hold action at least once. */
    private boolean held;
    private long interval;
    private boolean dimmed;

    public UpIconButton(Context context) {
        this(context, null);
    }

    public UpIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        int padding = Math.round(10 * dp);
        setPadding(padding, padding, padding, padding);
        setScaleType(ImageView.ScaleType.FIT_CENTER);
        setClickable(true);
        setFocusable(true);
        ImageViewCompat.setImageTintList(this,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.round(44 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    public boolean performClick() {
        Haptics.tap(this);
        return super.performClick();
    }

    /**
     * Makes the button repeat while it is held down: the action runs after the long-press time, then again and
     * again, faster and faster, until it returns {@code false} or the finger lifts or leaves the button. A tap that
     * ends before the long-press time is still only a click, and so is a hold whose first run returns {@code false}.
     */
    public void setOnHoldListener(BooleanSupplier action) {
        holdAction = action;
    }

    // The click is still made by super.onTouchEvent, which calls performClick.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (holdAction != null && isEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    held = false;
                    interval = FIRST_INTERVAL_MS;
                    postDelayed(repeater, ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isInside(event)) removeCallbacks(repeater);
                    break;
                case MotionEvent.ACTION_UP:
                    removeCallbacks(repeater);
                    if (held) {
                        // The hold has done the steps already, so letting go must not count as one more tap.
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        boolean handled = super.onTouchEvent(cancel);
                        cancel.recycle();
                        return handled;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    removeCallbacks(repeater);
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(repeater);
        super.onDetachedFromWindow();
    }

    private boolean isInside(MotionEvent event) {
        return event.getX() >= 0 && event.getX() < getWidth() && event.getY() >= 0 && event.getY() < getHeight();
    }

    /** Shows the button as unavailable. It stays tappable, so the tap can still be answered. */
    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        updateAlpha();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        updateAlpha();
    }

    private void updateAlpha() {
        setAlpha(dimmed ? 0.3f : (isPressed() ? 0.5f : 1f));
    }
}
