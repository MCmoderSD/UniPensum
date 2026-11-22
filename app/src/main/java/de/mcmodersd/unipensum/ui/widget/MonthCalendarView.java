package de.mcmodersd.unipensum.ui.widget;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.*;
import static android.view.MotionEvent.*;
import static android.view.ViewGroup.LayoutParams.*;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.*;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.drawable.*;
import static de.mcmodersd.unipensum.R.string.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;

import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * A month grid with previous/next navigation. Days outside {@link #setRange} (and weekends, if
 * {@link #setWeekdaysOnly} is on) cannot be selected; in weekdays-only mode they are not drawn at all.
 */
public class MonthCalendarView extends LinearLayout {

    public interface OnDateSelectedListener {
        void onDateSelected(LocalDate date);
    }

    private static final float FOCUS_WIDTH_DP = 2;
    /** The gap between a day and the ring of the focus around it. */
    private static final float FOCUS_GAP_DP = 3;

    private final TextView monthTitle;
    private final UpIconButton previous;
    private final UpIconButton next;
    private final Grid grid;

    private YearMonth month = YearMonth.now();
    private LocalDate min = LocalDate.of(2000, 1, 1);
    private LocalDate max = LocalDate.of(2100, 12, 31);
    private LocalDate selected;
    private boolean weekdaysOnly;
    private OnDateSelectedListener listener;

    public MonthCalendarView(Context context) {
        this(context, null);
    }

    public MonthCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        var header = new LinearLayout(context);
        header.setGravity(CENTER_VERTICAL);
        previous = new UpIconButton(context);
        previous.setImageResource(ic_chevron_left);
        previous.setContentDescription(context.getString(calendar_previous_month));
        previous.setOnClickListener(v -> showMonth(month.minusMonths(1)));
        next = new UpIconButton(context);
        next.setImageResource(ic_chevron_right);
        next.setContentDescription(context.getString(calendar_next_month));
        next.setOnClickListener(v -> showMonth(month.plusMonths(1)));

        monthTitle = new TextView(context);
        monthTitle.setGravity(CENTER);
        monthTitle.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        monthTitle.setTextSize(COMPLEX_UNIT_SP, 17);
        monthTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        monthTitle.setTextColor(ContextCompat.getColor(context, text_primary));

        header.addView(previous);
        header.addView(monthTitle, new LayoutParams(0, WRAP_CONTENT, 1f));
        header.addView(next);
        addView(header, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        grid = new Grid(context);
        addView(grid, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        refresh();
    }

    public void setRange(LocalDate min, LocalDate max) {
        this.min = min;
        this.max = max;
        refresh();
    }

    public void setWeekdaysOnly(boolean weekdaysOnly) {
        this.weekdaysOnly = weekdaysOnly;
        refresh();
    }

    /** Marks the date and shows its month. */
    public void setSelected(LocalDate date) {
        this.selected = date;
        if (date != null) month = YearMonth.from(date);
        refresh();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    private void showMonth(YearMonth target) {
        if (target.isBefore(YearMonth.from(min)) || target.isAfter(YearMonth.from(max))) return;
        Haptics.segmentTick(this);
        month = target;
        refresh();
    }

    private void refresh() {
        monthTitle.setText(TimeFormat.monthYear(getContext(), month));
        var hasPrevious = month.isAfter(YearMonth.from(min));
        var hasNext = month.isBefore(YearMonth.from(max));
        previous.setEnabled(hasPrevious);
        previous.setVisibility(hasPrevious ? VISIBLE : INVISIBLE);
        next.setEnabled(hasNext);
        next.setVisibility(hasNext ? VISIBLE : INVISIBLE);
        grid.requestLayout();
        grid.invalidate();
        // The days are other ones in another month.
        grid.accessibility.invalidateRoot();
    }

    private boolean isSelectable(LocalDate day) {
        return !day.isBefore(min) && !day.isAfter(max);
    }

    private final class Grid extends View {

        private final Paint textPaint = new Paint(ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(ANTI_ALIAS_FLAG);
        private final Paint focusPaint = new Paint(ANTI_ALIAS_FLAG);
        private final float dp = getResources().getDisplayMetrics().density;
        private final float cellHeight = 48 * dp;
        private final float labelHeight = 28 * dp;
        private final int textColor;
        private final int disabledColor;
        private final int onAccent;
        private float downX;
        private float downY;

        private final DayAccessibility accessibility = new DayAccessibility();

        Grid(Context context) {
            super(context);
            ViewCompat.setAccessibilityDelegate(this, accessibility);
            // The keys move between the days; the day that has the focus carries the ring, not the whole grid.
            setFocusable(true);
            setDefaultFocusHighlightEnabled(false);
            textColor = ContextCompat.getColor(context, text_primary);
            disabledColor = ContextCompat.getColor(context, divider);
            onAccent = ContextCompat.getColor(context, text_on_accent);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(TypedValue.applyDimension(COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setColor(ContextCompat.getColor(context, text_secondary));
            labelPaint.setTextSize(TypedValue.applyDimension(COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
            fillPaint.setColor(ContextCompat.getColor(context, accent));
            ringPaint.setColor(ContextCompat.getColor(context, accent));
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(1.5f * dp);
            focusPaint.setColor(ContextCompat.getColor(context, accent));
            focusPaint.setStyle(Paint.Style.STROKE);
            focusPaint.setStrokeWidth(FOCUS_WIDTH_DP * dp);
        }

        private int columns() {
            return weekdaysOnly ? 5 : 7;
        }

        /** Column of the day, Monday = 0. */
        private int columnOf(LocalDate day) {
            return day.getDayOfWeek().getValue() - 1;
        }

        /** Where a column is drawn, counted from the left: Monday is on the right when the layout runs from right to left. */
        private int slot(int column) {
            return Mirror.isRtl(this) ? columns() - 1 - column : column;
        }

        /** Row of the day within the month, counted from the week of the 1st. */
        private int weekRowOf(LocalDate day) {
            var offset = columnOf(month.atDay(1));
            return (offset + day.getDayOfMonth() - 1) / 7;
        }

        /** In weekdays-only mode a month can start on a weekend, which leaves its first week row empty. */
        private int firstVisibleRow() {
            for (var d = 1; d <= month.lengthOfMonth(); d++) {
                var day = month.atDay(d);
                if (columnOf(day) < columns()) return weekRowOf(day);
            }
            return 0;
        }

        /** A month never needs more than six week rows. */
        private static final int MAX_ROWS = 6;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Always reserve the maximum, so the sheet keeps its height and the month arrows do not jump.
            var height = Math.round(labelHeight + MAX_ROWS * cellHeight);
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            var locale = TimeFormat.locale(getContext());
            var cellWidth = getWidth() / (float) columns();

            for (var c = 0; c < columns(); c++) {
                var label = DayOfWeek.of(c + 1).getDisplayName(TextStyle.SHORT, locale);
                canvas.drawText(label, cellWidth * (slot(c) + 0.5f), labelHeight * 0.7f, labelPaint);
            }

            var today = LocalDate.now();
            for (var d = 1; d <= month.lengthOfMonth(); d++) {
                var day = month.atDay(d);
                var column = columnOf(day);
                if (column >= columns()) continue;
                var centerX = cellWidth * (slot(column) + 0.5f);
                var centerY = labelHeight + (weekRowOf(day) - firstVisibleRow() + 0.5f) * cellHeight;
                var radius = Math.min(cellWidth, cellHeight) * 0.42f;

                var enabled = isSelectable(day);
                var isSelected = day.equals(selected);
                if (isSelected) canvas.drawCircle(centerX, centerY, radius, fillPaint);
                else if (day.equals(today)) canvas.drawCircle(centerX, centerY, radius, ringPaint);
                // The ring of the keyboard focus sits outside the day, so it shows on a chosen day as well.
                if (isFocused() && accessibility.getKeyboardFocusedVirtualViewId() == d) {
                    canvas.drawCircle(centerX, centerY, radius + FOCUS_GAP_DP * dp, focusPaint);
                }

                textPaint.setColor(isSelected ? onAccent : (enabled ? textColor : disabledColor));
                textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                var fm = textPaint.getFontMetrics();
                canvas.drawText(String.valueOf(d), centerX, centerY - (fm.ascent + fm.descent) / 2f, textPaint);
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
                        select(event.getX(), event.getY());
                        performClick();
                    }
                }
            }
            return true;
        }

        @Override
        public boolean dispatchHoverEvent(MotionEvent event) {
            return accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
        }

        /**
         * The arrow keys move between the days and Enter chooses one. Tab is not for going from day to day, which
         * would take up to 31 presses to get past the grid: it goes on to the next control, so the grid is one stop.
         */
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_TAB) return super.dispatchKeyEvent(event);
            return accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
            accessibility.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
            // The focus always arrives on the chosen day: that is where one moves on from.
            if (gainFocus) {
                var start = startDay();
                if (start != ExploreByTouchHelper.INVALID_ID) accessibility.requestKeyboardFocusForVirtualView(start);
            }
        }

        /** The chosen day if this month shows it, else the first day that is shown. */
        private int startDay() {
            if (selected != null && YearMonth.from(selected).equals(month) && columnOf(selected) < columns()) {
                return selected.getDayOfMonth();
            }
            for (var d = 1; d <= month.lengthOfMonth(); d++) {
                if (columnOf(month.atDay(d)) < columns()) return d;
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        /** @return the day drawn at that point, or {@code null} for gaps and the weekday labels */
        private LocalDate dayAt(float x, float y) {
            if (y < labelHeight || x < 0 || x >= getWidth()) return null;
            var column = slot((int) (x / (getWidth() / (float) columns())));
            var row = (int) ((y - labelHeight) / cellHeight) + firstVisibleRow();
            for (var d = 1; d <= month.lengthOfMonth(); d++) {
                var day = month.atDay(d);
                if (columnOf(day) == column && weekRowOf(day) == row) return day;
            }
            return null;
        }

        private void select(float x, float y) {
            var day = dayAt(x, y);
            if (day == null) return;
            choose(day);
        }

        private void choose(LocalDate day) {
            if (!isSelectable(day)) {
                Haptics.reject(this);
                return;
            }
            Haptics.segmentTick(this);
            selected = day;
            invalidate();
            if (listener != null) listener.onDateSelected(day);
        }

        private void cellBounds(LocalDate day, Rect out) {
            var cellWidth = getWidth() / (float) columns();
            var left = Math.round(cellWidth * slot(columnOf(day)));
            var top = Math.round(labelHeight + (weekRowOf(day) - firstVisibleRow()) * cellHeight);
            out.set(left, top, Math.round(left + cellWidth), Math.round(top + cellHeight));
        }

        /** Lets TalkBack step through the days of the month; the virtual id is the day of month. */
        private final class DayAccessibility extends ExploreByTouchHelper {

            DayAccessibility() {
                super(Grid.this);
            }

            @Override
            protected int getVirtualViewAt(float x, float y) {
                var day = dayAt(x, y);
                return day == null ? INVALID_ID : day.getDayOfMonth();
            }

            @Override
            protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
                for (var d = 1; d <= month.lengthOfMonth(); d++) {
                    if (columnOf(month.atDay(d)) < columns()) virtualViewIds.add(d);
                }
            }

            @Override
            protected void onPopulateNodeForVirtualView(int id, @NonNull AccessibilityNodeInfoCompat node) {
                var day = month.atDay(id);
                var context = getContext();
                var bounds = new Rect();
                cellBounds(day, bounds);
                setBoundsInScreenFromBoundsInParent(node, bounds);
                node.setContentDescription(
                        day.getDayOfWeek().getDisplayName(TextStyle.FULL, TimeFormat.locale(context))
                                + ", " + TimeFormat.dateMedium(context, day)
                );
                node.setCheckable(true);
                node.setChecked(day.equals(selected) ? CHECKED_STATE_TRUE : CHECKED_STATE_FALSE);
                node.setEnabled(isSelectable(day));
                node.addAction(ACTION_CLICK);
            }

            @Override
            protected boolean onPerformActionForVirtualView(int id, int action, Bundle arguments) {
                if (action != ACTION_CLICK) return false;
                choose(month.atDay(id));
                return true;
            }
        }
    }
}