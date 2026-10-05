package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    void setScrollListener(OnScrollListener listener) {
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> listener.onScrolled(this, y));
    }

    int scrollY() {
        return scroll.getScrollY();
    }

    /** Applies the scroll position once the grid has been laid out. */
    void scrollToWhenReady(int y) {
        scroll.post(() -> {
            if (scroll.getScrollY() != y) scroll.scrollTo(0, y);
        });
    }
}
