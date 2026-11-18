package de.mcmodersd.unipensum.ui.widget;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.CENTER;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE;
import static de.mcmodersd.unipensum.R.color.*;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;

import java.util.ArrayList;

/**
 * A row of mutually exclusive options. The selection is a pill in the accent color that slides to
 * the chosen segment, while the labels cross-fade between their two colors along the way. Each segment is
 * 48dp tall, the touch size Android asks for; the pill is drawn 3dp inside it. For a screen reader the control is
 * a group of radio buttons.
 */
public class UpSegmentedControl extends LinearLayout {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int index);
    }

    private static final long SLIDE_MILLIS = 260;
    private static final int SEGMENT_HEIGHT_DP = 48;
    private static final int PILL_INSET_DP = 3;
    private static final int PILL_RADIUS_DP = 11;

    private final ArrayList<TextView> segments = new ArrayList<>();
    private final Paint pillPaint = new Paint(ANTI_ALIAS_FLAG);
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
        setPadding(Math.round(PILL_INSET_DP * dp), 0, Math.round(PILL_INSET_DP * dp), 0);
        var background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, surface));
        setBackground(background);
        setWillNotDraw(false);

        pillPaint.setColor(ContextCompat.getColor(context, accent));
        colorSelected = ContextCompat.getColor(context, text_on_accent);
        colorIdle = ContextCompat.getColor(context, text_secondary);
    }

    public void setOptions(CharSequence... labels) {
        removeAllViews();
        segments.clear();
        for (var i = 0; i < labels.length; i++) {
            var segment = new TextView(getContext());
            segment.setText(labels[i]);
            segment.setGravity(CENTER);
            segment.setMaxLines(1);
            segment.setTextSize(COMPLEX_UNIT_SP, 14);
            segment.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            segment.setMinHeight(Math.round(SEGMENT_HEIGHT_DP * dp));
            segment.setPadding(Math.round(4 * dp), 0, Math.round(4 * dp), 0);
            segment.setClickable(true);
            segment.setFocusable(true);
            FocusRing.install(segment, PILL_RADIUS_DP, PILL_INSET_DP);
            ViewCompat.setAccessibilityDelegate(segment, new SegmentAccessibility(i));
            final var index = i;
            segment.setOnClickListener(v -> select(index));
            addView(segment, new LayoutParams(0, WRAP_CONTENT, 1f));
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
        for (var i = 0; i < segments.size(); i++) {
            // 1 when the pill sits exactly on this segment, 0 once it is a full segment away.
            var coverage = Math.max(0f, 1f - Math.abs(position - i));
            segments.get(i).setTextColor(ColorUtils.blendARGB(colorIdle, colorSelected, coverage));
            segments.get(i).setSelected(i == selected);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (segments.isEmpty()) return;
        // Interpolate between the two neighboring segments, which also keeps right-to-left layouts correct.
        var from = Math.max(0, Math.min(segments.size() - 1, (int) Math.floor(position)));
        var to = Math.min(segments.size() - 1, from + 1);
        var fraction = position - from;
        var a = segments.get(from);
        var b = segments.get(to);
        var left = a.getLeft() + (b.getLeft() - a.getLeft()) * fraction;
        var right = a.getRight() + (b.getRight() - a.getRight()) * fraction;
        pillRect.set(left, PILL_INSET_DP * dp, right, getHeight() - PILL_INSET_DP * dp);
        canvas.drawRoundRect(pillRect, PILL_RADIUS_DP * dp, PILL_RADIUS_DP * dp, pillPaint);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(RadioGroup.class.getName());
        AccessibilityNodeInfoCompat.wrap(info).setCollectionInfo(
                new CollectionInfoCompat.Builder()
                        .setRowCount(1)
                        .setColumnCount(segments.size())
                        .setHierarchical(false)
                        .setSelectionMode(SELECTION_MODE_SINGLE)
                        .build()
        );
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
        for (var segment : segments) segment.setEnabled(enabled);
    }

    /** A segment is a radio button of the group: it says whether it is the chosen one, and which one of how many. */
    private static final class SegmentAccessibility extends AccessibilityDelegateCompat {

        private final int index;

        SegmentAccessibility(int index) {
            this.index = index;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(RadioButton.class.getName());
            info.setCheckable(true);
            info.setChecked(host.isSelected());
            info.setCollectionItemInfo(
                    new CollectionItemInfoCompat.Builder()
                            .setRowIndex(0)
                            .setRowSpan(1)
                            .setColumnIndex(index)
                            .setColumnSpan(1)
                            .setSelected(host.isSelected())
                            .build()
            );
        }
    }
}