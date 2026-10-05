package de.mcmodersd.unipensum.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.R;

/**
 * A row of mutually exclusive options. The selection is a pill in the accent color that slides to
 * the chosen segment, while the labels cross-fade between their two colors along the way.
 */
public class UpSegmentedControl extends LinearLayout {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int index);
    }

    private static final long SLIDE_MILLIS = 260;

    private final List<TextView> segments = new ArrayList<>();
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pillRect = new RectF();
    private final float dp = getResources().getDisplayMetrics().density;
    private final int colorSelected;
    private final int colorIdle;
    private int selected = 0;
    /** Where the pill currently is, as a fractional segment index; equals {@link #selected} at rest. */
    private float position = 0f;
    private ValueAnimator slide;
    private OnSelectionChangedListener listener;

    public UpSegmentedControl(Context context) {
        this(context, null);
    }

    public UpSegmentedControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        int padding = Math.round(3 * dp);
        setPadding(padding, padding, padding, padding);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, R.color.surface));
        setBackground(background);
        setWillNotDraw(false);

        pillPaint.setColor(ContextCompat.getColor(context, R.color.accent));
        colorSelected = ContextCompat.getColor(context, R.color.text_on_accent);
        colorIdle = ContextCompat.getColor(context, R.color.text_secondary);
    }

    public void setOptions(CharSequence... labels) {
        removeAllViews();
        segments.clear();
        for (int i = 0; i < labels.length; i++) {
            TextView segment = new TextView(getContext());
            segment.setText(labels[i]);
            segment.setGravity(Gravity.CENTER);
            segment.setMaxLines(1);
            segment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            segment.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            segment.setMinHeight(Math.round(42 * dp));
            segment.setPadding(Math.round(4 * dp), 0, Math.round(4 * dp), 0);
            segment.setClickable(true);
            segment.setFocusable(true);
            final int index = i;
            segment.setOnClickListener(v -> select(index));
            addView(segment, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            segments.add(segment);
        }
        setSelectedIndex(Math.min(selected, Math.max(0, labels.length - 1)));
    }

    public int getSelectedIndex() {
        return selected;
    }

    /** Selects without animation and without notifying the listener. */
    public void setSelectedIndex(int index) {
        if (slide != null) slide.cancel();
        selected = index;
        moveTo(index);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    /** A tap by the user: slides the pill over and reports the change. */
    private void select(int index) {
        if (index == selected) return;
        selected = index;
        Haptics.segmentTick(this);

        if (slide != null) slide.cancel();
        slide = ValueAnimator.ofFloat(position, index);
        slide.setDuration(SLIDE_MILLIS);
        slide.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        slide.addUpdateListener(animation -> moveTo((float) animation.getAnimatedValue()));
        slide.start();

        if (listener != null) listener.onSelectionChanged(index);
    }

    private void moveTo(float newPosition) {
        position = newPosition;
        for (int i = 0; i < segments.size(); i++) {
            // 1 when the pill sits exactly on this segment, 0 once it is a full segment away.
            float coverage = Math.max(0f, 1f - Math.abs(position - i));
            segments.get(i).setTextColor(ColorUtils.blendARGB(colorIdle, colorSelected, coverage));
            segments.get(i).setSelected(i == selected);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (segments.isEmpty()) return;
        // Interpolate between the two neighbouring segments, which also keeps right-to-left layouts correct.
        int from = Math.max(0, Math.min(segments.size() - 1, (int) Math.floor(position)));
        int to = Math.min(segments.size() - 1, from + 1);
        float fraction = position - from;
        View a = segments.get(from);
        View b = segments.get(to);
        float left = a.getLeft() + (b.getLeft() - a.getLeft()) * fraction;
        float right = a.getRight() + (b.getRight() - a.getRight()) * fraction;
        pillRect.set(left, getPaddingTop(), right, getHeight() - getPaddingBottom());
        canvas.drawRoundRect(pillRect, 11 * dp, 11 * dp, pillPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (slide != null) slide.cancel();
        moveTo(selected);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (View segment : segments) segment.setEnabled(enabled);
    }
}
