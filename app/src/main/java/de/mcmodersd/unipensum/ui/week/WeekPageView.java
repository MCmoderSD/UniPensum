package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.domain.model.NameStyle;

/** One pager page: the day header on top, the scrollable grid below. */
// Created in code by WeekPagerAdapter only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class WeekPageView extends LinearLayout {

    interface OnScrollListener {
        void onScrolled(WeekPageView page, int scrollY);
    }

    private final WeekHeaderView header;
    private final ScrollView scroll;
    private final WeekGridView grid;
    /** True while {@link #followWhenReady} moves the grid. */
    private boolean following;

    WeekPageView(Context context, GridMetrics metrics) {
        super(context);
        setOrientation(VERTICAL);

        header = new WeekHeaderView(context, metrics);
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        grid = new WeekGridView(context, metrics);
        scroll.addView(grid, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    }

    /** @param timetable {@code null} while loading; the grid then stays empty */
    void bind(LocalDate monday, LocalDate today, TimeWindow window, NameStyle nameStyle, Timetable timetable,
              WeekGridView.OnSessionClickListener clickListener) {
        header.bind(monday, today);
        List<List<SessionView>> perDay = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            perDay.add(timetable == null ? List.of() : timetable.on(monday.plusDays(i)));
        }
        grid.bind(monday, window, nameStyle, perDay, clickListener);
    }

    /** Reports every scroll of this page, except the ones {@link #followWhenReady} makes. */
    void setScrollListener(OnScrollListener listener) {
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            if (!following) listener.onScrolled(this, y);
        });
    }

    int scrollY() {
        return scroll.getScrollY();
    }

    /**
     * Moves the grid to the position {@code y} gives once the grid has been laid out. The position is asked for when
     * the move happens, not when it is requested, and the move is not reported as a scroll of this page: another
     * page is the one being scrolled, and a report would send it back to an outdated position in the middle of a
     * fling.
     */
    void followWhenReady(IntSupplier y) {
        scroll.post(() -> {
            int target = y.getAsInt();
            if (scroll.getScrollY() == target) return;
            following = true;
            try {
                scroll.scrollTo(0, target);
            } finally {
                following = false;
            }
        });
    }
}
