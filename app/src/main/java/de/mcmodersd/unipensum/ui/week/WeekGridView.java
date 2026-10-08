package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.logic.NowIndicator;
import de.mcmodersd.unipensum.domain.logic.WeekLayout;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * The hour lines, the time axis and the session blocks of one week (Monday to Friday).
 * The height is the window length times the hour height: at least the minimum hour height,
 * or more if the scrolling area offers it, so the day fills the screen whenever it comfortably fits.
 * Zooming stretches the hours further.
 * <p>
 * In the current week, a line marks the current time: faint across the whole week, solid in the column of
 * today, drawn over the session blocks. The label of the current hour is highlighted. The marker moves with
 * the clock, once a minute.
 */
// Created in code by WeekPageView only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class WeekGridView extends ViewGroup {

    interface OnSessionClickListener {
        void onSessionClick(SessionView session);
    }

    private static final class Placed {
        final SessionBlockView view;
        final int dayIndex;
        final WeekLayout.Placement placement;
        final int startMin;
        final int endMin;
        /** Position inside the grid, computed in onMeasure. */
        final Rect bounds = new Rect();

        Placed(SessionBlockView view, int dayIndex, WeekLayout.Placement placement, int startMin, int endMin) {
            this.view = view;
            this.dayIndex = dayIndex;
            this.placement = placement;
            this.startMin = startMin;
            this.endMin = endMin;
        }
    }

    private final GridMetrics metrics;
    private final Paint linePaint = new Paint();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nowLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nowLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nowFaintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Placed> placed = new ArrayList<>();
    private final Runnable tick = () -> {
        updateNow();
        scheduleTick();
    };
    private TimeWindow window = TimeWindow.DEFAULT;
    private float zoom = 1f;
    private int viewportHeight;
    private LocalDate monday;
    /** Where the marker of the current time is drawn, {@code null} if it is not part of this week or these hours. */
    private NowIndicator.Position now;

    WeekGridView(Context context, GridMetrics metrics) {
        super(context);
        this.metrics = metrics;
        setWillNotDraw(false);

        linePaint.setColor(ContextCompat.getColor(context, R.color.divider));
        linePaint.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density * 0.75f));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        // The marker is black on the light theme and white on the dark one, like the rest of the app.
        var nowColor = ContextCompat.getColor(context, R.color.accent);
        nowLabelPaint.set(labelPaint);
        nowLabelPaint.setColor(nowColor);
        nowLabelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        nowLinePaint.setColor(nowColor);
        nowLinePaint.setStrokeWidth(metrics.nowLine);
        nowFaintPaint.setColor(ColorUtils.setAlphaComponent(nowColor, 0x66));
        nowFaintPaint.setStrokeWidth(Math.max(1f, metrics.nowLine / 2f));
    }

    /**
     * @param monday the Monday of the week shown, which tells whether the current time belongs to it
     * @param perDay exactly five lists, Monday to Friday, each ordered by start time
     */
    void bind(LocalDate monday, TimeWindow window, NameStyle nameStyle, List<List<SessionView>> perDay,
              OnSessionClickListener listener) {
        this.monday = monday;
        this.window = window;
        updateNow();
        removeAllViews();
        placed.clear();

        var windowStart = window.startHour() * 60;
        var windowEnd = window.endHour() * 60;
        for (var day = 0; day < perDay.size(); day++) {
            var sessions = perDay.get(day);
            var blocks = new ArrayList<WeekLayout.Block>();
            for (var session : sessions) {
                blocks.add(new WeekLayout.Block(session.session().id(),
                        session.session().details().startMin(), session.session().details().endMin()));
            }
            for (var placement : WeekLayout.layout(blocks)) {
                var session = find(sessions, placement.id());
                var start = Math.max(session.session().details().startMin(), windowStart);
                var end = Math.min(session.session().details().endMin(), windowEnd);
                if (end <= start) continue;

                var block = new SessionBlockView(getContext(), metrics, session, nameStyle,
                        placement.overlapping());
                if (listener != null) block.setOnClickListener(v -> listener.onSessionClick(session));
                addView(block);
                placed.add(new Placed(block, day, placement, start, end));
            }
        }
        requestLayout();
        invalidate();
    }

    // --- the marker of the current time ---

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateNow();
        scheduleTick();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    /** The next full minute, so the marker keeps in step with the clock instead of drifting. */
    private void scheduleTick() {
        removeCallbacks(tick);
        postDelayed(tick, 60_000 - System.currentTimeMillis() % 60_000 + 50);
    }

    private void updateNow() {
        NowIndicator.Position next = monday == null ? null
                : NowIndicator.at(monday, LocalDateTime.now(), window.startHour(), window.endHour()).orElse(null);
        if (Objects.equals(next, now)) return;
        now = next;
        invalidate();
    }

    private static SessionView find(List<SessionView> sessions, long id) {
        for (var session : sessions) {
            if (session.session().id() == id) return session;
        }
        throw new IllegalStateException("Layout returned an unknown session " + id);
    }

    private int hours() {
        return window.endHour() - window.startHour();
    }

    /** How much the hours are stretched: 1 is the normal grid, 2 doubles the height of every hour. */
    float zoom() {
        return zoom;
    }

    void setZoom(float zoom) {
        if (this.zoom == zoom) return;
        this.zoom = zoom;
        requestLayout();
        invalidate();
    }

    /**
     * The height of the scrolling area the grid sits in, which the grid fills when the hours fit into it. The
     * parent knows it only after measuring the grid, so the page tells it.
     *
     * @return whether the height changed, which makes the grid need a new measure
     */
    boolean setViewportHeight(int height) {
        if (viewportHeight == height) return false;
        viewportHeight = height;
        requestLayout();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The grid fills the scrolling area, or is as high as the hours need at their minimum height. Zoom
        // stretches only the hours, not the space above the first and below the last line.
        var minHeight = hours() * metrics.minHourHeight + 2 * metrics.verticalPadding;
        var fitted = Math.max(viewportHeight, minHeight);
        var height = Math.round(2 * metrics.verticalPadding + (fitted - 2 * metrics.verticalPadding) * zoom);
        var width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);

        // The blocks are sized here, not in onLayout: fitting their content changes child views,
        // which must not happen during a layout pass.
        var hourHeight = hourHeight(height);
        var columnWidth = metrics.columnWidth(width);
        for (var item : placed) {
            var columnShare = columnWidth / item.placement.columns();
            var blockLeft = Math.round(metrics.gutter + item.dayIndex * columnWidth
                    + item.placement.column() * columnShare) + metrics.blockInset;
            var blockRight = Math.round(metrics.gutter + item.dayIndex * columnWidth
                    + (item.placement.column() + 1) * columnShare) - metrics.blockInset;
            var blockTop = Math.round(yOf(item.startMin, hourHeight)) + metrics.blockInset;
            var blockBottom = Math.round(yOf(item.endMin, hourHeight)) - metrics.blockInset;
            item.bounds.set(blockLeft, blockTop, Math.max(blockLeft, blockRight), Math.max(blockTop, blockBottom));

            item.view.fitTo(item.bounds.width(), item.bounds.height());
            item.view.measure(
                    MeasureSpec.makeMeasureSpec(item.bounds.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(item.bounds.height(), MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (var item : placed) {
            item.view.layout(item.bounds.left, item.bounds.top, item.bounds.right, item.bounds.bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var hourHeight = hourHeight(getHeight());
        var labelOffset = (labelPaint.descent() + labelPaint.ascent()) / 2f;

        for (var i = 0; i <= hours(); i++) {
            var y = metrics.verticalPadding + i * hourHeight;
            canvas.drawLine(metrics.gutter, y, getWidth(), y, linePaint);
            var hour = window.startHour() + i;
            var label = TimeFormat.hour(getContext(), hour);
            var paint = now != null && now.minutes() / 60 == hour ? nowLabelPaint : labelPaint;
            canvas.drawText(label, metrics.gutter - 6 * getResources().getDisplayMetrics().density,
                    y - labelOffset, paint);
        }
        var columnWidth = metrics.columnWidth(getWidth());
        for (var i = 0; i <= 5; i++) {
            var x = metrics.gutter + i * columnWidth;
            canvas.drawLine(x, metrics.verticalPadding, x, getHeight() - metrics.verticalPadding, linePaint);
        }
    }

    /** Over the session blocks, so the current time stays readable inside a lecture. */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (now == null) return;
        var y = yOf(now.minutes(), hourHeight(getHeight()));
        var columnWidth = metrics.columnWidth(getWidth());
        var left = metrics.gutter + now.dayIndex() * columnWidth;
        canvas.drawLine(metrics.gutter, y, getWidth(), y, nowFaintPaint);
        canvas.drawLine(left, y, left + columnWidth, y, nowLinePaint);
    }

    private float hourHeight(int totalHeight) {
        return (totalHeight - 2f * metrics.verticalPadding) / hours();
    }

    private float yOf(int minutes, float hourHeight) {
        return metrics.verticalPadding + (minutes - window.startHour() * 60) / 60f * hourHeight;
    }
}
