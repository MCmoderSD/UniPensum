package de.mcmodersd.unipensum.ui.widget;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.*;
import static de.mcmodersd.unipensum.R.color.*;

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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

public class UpSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private static final int WIDTH_DP = 52;
    private static final int TRACK_HEIGHT_DP = 32;
    private static final int TOUCH_HEIGHT_DP = 48;
    private static final int THUMB_INSET_DP = 3;

    private final Paint trackPaint = new Paint(ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(ANTI_ALIAS_FLAG);
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
        trackOn = ContextCompat.getColor(context, accent);
        trackOff = ContextCompat.getColor(context, track_off);
        thumbOn = ContextCompat.getColor(context, text_on_accent);
        thumbOff = ContextCompat.getColor(context, sheet);
        setClickable(true);
        setFocusable(true);
        FocusRing.install(this, TRACK_HEIGHT_DP / 2f, (TOUCH_HEIGHT_DP - TRACK_HEIGHT_DP) / 2f);
    }

    /**
     * Makes the whole row a switch: a tap anywhere on it toggles {@code toggle}. A screen reader only gets the
     * switch, which carries the name of the row, so the row is not read a second time.
     */
    public static void bindRow(View row, UpSwitch toggle) {
        row.setOnClickListener(v -> toggle.performClick());
        row.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
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
        setMeasuredDimension(Math.round(WIDTH_DP * dp), Math.round(TOUCH_HEIGHT_DP * dp));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var radius = TRACK_HEIGHT_DP * dp / 2f;
        var top = (getHeight() - TRACK_HEIGHT_DP * dp) / 2f;
        track.set(0, top, getWidth(), top + TRACK_HEIGHT_DP * dp);
        trackPaint.setColor(ColorUtils.blendARGB(trackOff, trackOn, progress));
        canvas.drawRoundRect(track, radius, radius, trackPaint);

        var thumbRadius = radius - THUMB_INSET_DP * dp;
        var endX = getWidth() - radius;
        var thumbX = radius + (endX - radius) * progress;
        thumbPaint.setColor(ColorUtils.blendARGB(thumbOff, thumbOn, progress));
        canvas.drawCircle(Mirror.isRtl(this) ? getWidth() - thumbX : thumbX, top + radius, thumbRadius, thumbPaint);
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
        AccessibilityNodeInfoCompat.wrap(info).setChecked(
                checked ? CHECKED_STATE_TRUE : CHECKED_STATE_FALSE
        );
    }
}