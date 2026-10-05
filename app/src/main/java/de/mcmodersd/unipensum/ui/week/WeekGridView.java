package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.logic.WeekLayout;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * The hour lines, the time axis and the session blocks of one week (Monday to Friday).
 * The height is the window length times the hour height: at least the minimum hour height,
 * or more if the parent offers it (ScrollView with fillViewport), so the day fills the screen
 * whenever it comfortably fits.
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
    private final List<Placed> placed = new ArrayList<>();
    private TimeWindow window = TimeWindow.DEFAULT;

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
    }

    /**
     * @param perDay exactly five lists, Monday to Friday, each ordered by start time
     */
    void bind(TimeWindow window, NameStyle nameStyle, List<List<SessionView>> perDay, OnSessionClickListener listener) {
        this.window = window;
        removeAllViews();
        placed.clear();

        int windowStart = window.startHour() * 60;
        int windowEnd = window.endHour() * 60;
        for (int day = 0; day < perDay.size(); day++) {
            List<SessionView> sessions = perDay.get(day);
            List<WeekLayout.Block> blocks = new ArrayList<>();
            for (SessionView session : sessions) {
                blocks.add(new WeekLayout.Block(session.session().id(),
                        session.session().details().startMin(), session.session().details().endMin()));
            }
            for (WeekLayout.Placement placement : WeekLayout.layout(blocks)) {
                SessionView session = find(sessions, placement.id());
                int start = Math.max(session.session().details().startMin(), windowStart);
                int end = Math.min(session.session().details().endMin(), windowEnd);
                if (end <= start) continue;

                SessionBlockView block = new SessionBlockView(getContext(), metrics, session, nameStyle,
                        placement.overlapping());
                if (listener != null) block.setOnClickListener(v -> listener.onSessionClick(session));
                addView(block);
                placed.add(new Placed(block, day, placement, start, end));
            }
        }
        requestLayout();
        invalidate();
    }

    private static SessionView find(List<SessionView> sessions, long id) {
        for (SessionView session : sessions) {
            if (session.session().id() == id) return session;
        }
        throw new IllegalStateException("Layout returned an unknown session " + id);
    }

    private int hours() {
        return window.endHour() - window.startHour();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minHeight = hours() * metrics.minHourHeight + 2 * metrics.verticalPadding;
        int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
                ? Math.max(MeasureSpec.getSize(heightMeasureSpec), minHeight)
                : minHeight;
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);

        // The blocks are sized here, not in onLayout: fitting their content changes child views,
        // which must not happen during a layout pass.
        float hourHeight = hourHeight(height);
        float columnWidth = metrics.columnWidth(width);
        for (Placed item : placed) {
            float columnShare = columnWidth / item.placement.columns();
            int blockLeft = Math.round(metrics.gutter + item.dayIndex * columnWidth
                    + item.placement.column() * columnShare) + metrics.blockInset;
            int blockRight = Math.round(metrics.gutter + item.dayIndex * columnWidth
                    + (item.placement.column() + 1) * columnShare) - metrics.blockInset;
            int blockTop = Math.round(yOf(item.startMin, hourHeight)) + metrics.blockInset;
            int blockBottom = Math.round(yOf(item.endMin, hourHeight)) - metrics.blockInset;
            item.bounds.set(blockLeft, blockTop, Math.max(blockLeft, blockRight), Math.max(blockTop, blockBottom));

            item.view.fitTo(item.bounds.width(), item.bounds.height());
            item.view.measure(
                    MeasureSpec.makeMeasureSpec(item.bounds.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(item.bounds.height(), MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (Placed item : placed) {
            item.view.layout(item.bounds.left, item.bounds.top, item.bounds.right, item.bounds.bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float hourHeight = hourHeight(getHeight());
        float labelOffset = (labelPaint.descent() + labelPaint.ascent()) / 2f;

        for (int i = 0; i <= hours(); i++) {
            float y = metrics.verticalPadding + i * hourHeight;
            canvas.drawLine(metrics.gutter, y, getWidth(), y, linePaint);
            String label = TimeFormat.hour(getContext(), window.startHour() + i);
            canvas.drawText(label, metrics.gutter - 6 * getResources().getDisplayMetrics().density,
                    y - labelOffset, labelPaint);
        }
        float columnWidth = metrics.columnWidth(getWidth());
        for (int i = 0; i <= 5; i++) {
            float x = metrics.gutter + i * columnWidth;
            canvas.drawLine(x, metrics.verticalPadding, x, getHeight() - metrics.verticalPadding, linePaint);
        }
    }

    private float hourHeight(int totalHeight) {
        return (totalHeight - 2f * metrics.verticalPadding) / hours();
    }

    private float yOf(int minutes, float hourHeight) {
        return metrics.verticalPadding + (minutes - window.startHour() * 60) / 60f * hourHeight;
    }
}
