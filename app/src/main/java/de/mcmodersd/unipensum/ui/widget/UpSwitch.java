package de.mcmodersd.unipensum.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import de.mcmodersd.unipensum.R;

/** The app's own toggle: a pill track with a sliding thumb. */
public class UpSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final float dp = getResources().getDisplayMetrics().density;
    private final int trackOn;
    private final int trackOff;
    private final int thumbOn;
    private final int thumbOff;
    private boolean checked;
    private float progress;
    private OnCheckedChangeListener listener;

    public UpSwitch(Context context) {
        this(context, null);
    }

    public UpSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        trackOn = ContextCompat.getColor(context, R.color.accent);
        trackOff = ContextCompat.getColor(context, R.color.divider);
        thumbOn = ContextCompat.getColor(context, R.color.text_on_accent);
        thumbOff = ContextCompat.getColor(context, R.color.sheet);
        setClickable(true);
        setFocusable(true);
    }

    public boolean isChecked() {
        return checked;
    }

    /** Changes the state without animation and without notifying the listener. */
    public void setChecked(boolean checked) {
        this.checked = checked;
        progress = checked ? 1f : 0f;
        invalidate();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Math.round(52 * dp), Math.round(32 * dp));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var radius = getHeight() / 2f;
        track.set(0, 0, getWidth(), getHeight());
        trackPaint.setColor(ColorUtils.blendARGB(trackOff, trackOn, progress));
        canvas.drawRoundRect(track, radius, radius, trackPaint);

        var thumbRadius = radius - 3 * dp;
        var endX = getWidth() - radius;
        thumbPaint.setColor(ColorUtils.blendARGB(thumbOff, thumbOn, progress));
        canvas.drawCircle(radius + (endX - radius) * progress, radius, thumbRadius, thumbPaint);
    }

    @Override
    public boolean performClick() {
        checked = !checked;
        Haptics.toggle(this, checked);
        var animator = ValueAnimator.ofFloat(progress, checked ? 1f : 0f);
        animator.setDuration(160);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
        if (listener != null) listener.onCheckedChanged(checked);
        return super.performClick();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checked);
    }
}