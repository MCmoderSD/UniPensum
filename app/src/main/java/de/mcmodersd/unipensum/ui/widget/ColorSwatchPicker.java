package de.mcmodersd.unipensum.ui.widget;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.view.MotionEvent.*;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.*;
import static de.mcmodersd.unipensum.R.color.text_primary;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.domain.model.CourseColor.BLUE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.List;

import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.ui.format.CourseColors;

/** The course color presets as a grid of circles; the selected one gets a ring. */
public class ColorSwatchPicker extends View {

    public interface OnColorSelectedListener {
        @SuppressWarnings("unused")
        void onColorSelected(CourseColor color);
    }

    private static final int COLUMNS = 6;

    private final CourseColor[] colors = CourseColor.values();
    private final Paint fill = new Paint(ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(ANTI_ALIAS_FLAG);
    private final float dp = getResources().getDisplayMetrics().density;
    private final float cellHeight = 56 * dp;
    private final SwatchAccessibility accessibility = new SwatchAccessibility();
    private CourseColor selected = BLUE;
    private OnColorSelectedListener listener;
    private float downX;
    private float downY;

    public ColorSwatchPicker(Context context) {
        this(context, null);
    }

    public ColorSwatchPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2.5f * dp);
        ring.setColor(ContextCompat.getColor(context, text_primary));
        ViewCompat.setAccessibilityDelegate(this, accessibility);
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    public void setSelectedColor(CourseColor color) {
        selected = color;
        invalidate();
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    private int rows() {
        return (colors.length + COLUMNS - 1) / COLUMNS;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(rows() * cellHeight));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        var cellWidth = getWidth() / (float) COLUMNS;
        var radius = Math.min(cellWidth, cellHeight) * 0.34f;
        for (var i = 0; i < colors.length; i++) {
            var cx = cellWidth * (i % COLUMNS + 0.5f);
            var row = i / COLUMNS;
            var cy = cellHeight * (row + 0.5f);
            fill.setColor(CourseColors.resolve(getContext(), colors[i]));
            canvas.drawCircle(cx, cy, radius, fill);
            if (colors[i] == selected) canvas.drawCircle(cx, cy, radius + 5 * dp, ring);
        }
    }

    // The click is made by performClick, which this method calls itself.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case ACTION_DOWN -> {
                downX = event.getX();
                downY = event.getY();
            }
            case ACTION_UP -> {
                if (Math.abs(event.getX() - downX) < 12 * dp && Math.abs(event.getY() - downY) < 12 * dp) {
                    pick(indexAt(event.getX(), event.getY()));
                    performClick();
                }
            }
        }
        return true;
    }

    private int indexAt(float x, float y) {
        var column = Math.min(COLUMNS - 1, (int) (x / (getWidth() / (float) COLUMNS)));
        var index = (int) (y / cellHeight) * COLUMNS + column;
        return index >= 0 && index < colors.length ? index : -1;
    }

    private void pick(int index) {
        if (index < 0 || colors[index] == selected) return;
        selected = colors[index];
        Haptics.segmentTick(this);
        invalidate();
        if (listener != null) listener.onColorSelected(selected);
    }

    private void cellBounds(int index, Rect out) {
        var cellWidth = getWidth() / (float) COLUMNS;
        var left = Math.round(cellWidth * (index % COLUMNS));
        var row = index / COLUMNS;
        var top = Math.round(cellHeight * row);
        out.set(left, top, Math.round(left + cellWidth), Math.round(top + cellHeight));
    }

    private String colorName(CourseColor color) {
        var context = getContext();
        return switch (color) {
            case RED -> context.getString(color_red);
            case ORANGE -> context.getString(color_orange);
            case YELLOW -> context.getString(color_yellow);
            case GREEN -> context.getString(color_green);
            case TEAL -> context.getString(color_teal);
            case BLUE -> context.getString(color_blue);
            case VIOLET -> context.getString(color_violet);
            case PINK -> context.getString(color_pink);
            case GRAPHITE -> context.getString(color_graphite);
            case GRAY -> context.getString(color_gray);
            default -> context.getString(color_silver);
        };
    }

    /** Exposes every swatch as its own focusable, named element for TalkBack. */
    private final class SwatchAccessibility extends ExploreByTouchHelper {

        SwatchAccessibility() {
            super(ColorSwatchPicker.this);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            var index = indexAt(x, y);
            return index < 0 ? INVALID_ID : index;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (var i = 0; i < colors.length; i++) virtualViewIds.add(i);
        }

        @Override
        protected void onPopulateNodeForVirtualView(int id, @NonNull AccessibilityNodeInfoCompat node) {
            var bounds = new Rect();
            cellBounds(id, bounds);
            setBoundsInScreenFromBoundsInParent(node, bounds);
            node.setContentDescription(colorName(colors[id]));
            node.setCheckable(true);
            node.setChecked(colors[id] == selected ? CHECKED_STATE_TRUE : CHECKED_STATE_FALSE);
            node.addAction(ACTION_CLICK);
        }

        @Override
        protected boolean onPerformActionForVirtualView(int id, int action, Bundle arguments) {
            if (action != ACTION_CLICK) return false;
            pick(id);
            invalidateVirtualView(id);
            return true;
        }
    }
}