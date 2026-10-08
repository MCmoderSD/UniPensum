package de.mcmodersd.unipensum.ui.week;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
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
import de.mcmodersd.unipensum.ui.widget.Haptics;

/**
 * One pager page: the day header on top, the scrollable grid below. Two fingers stretch the hours of the grid,
 * keeping the time between the fingers in place; a double tap puts them back.
 */
// Created in code by WeekPagerAdapter only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class WeekPageView extends LinearLayout {

    interface OnScrollListener {
        void onScrolled(WeekPageView page, int scrollY);
    }

    interface OnZoomListener {
        /** @param settled false while the zoom still changes under the fingers or in the animation back */
        void onZoomed(WeekPageView page, float zoom, boolean settled);
    }

    private static final long RESET_DURATION_MS = 250;
    private static final int NO_SCROLL = Integer.MIN_VALUE;

    private final GridMetrics metrics;
    private final WeekHeaderView header;
    private final ScrollView scroll;
    private final WeekGridView grid;
    private final ScaleGestureDetector pinch;
    private final GestureDetector taps;
    private OnZoomListener zoomListener;
    /** True while {@link #followWhenReady} or {@link #followZoom} moves the grid. */
    private boolean following;
    /** True from the start of a pinch to the end of the touch: the fingers are no longer for the children. */
    private boolean swallowing;
    private ValueAnimator reset;
    /** Where this page scrolls once the grid has its new height after a zoom step. */
    private int pendingScroll = NO_SCROLL;

    WeekPageView(Context context, GridMetrics metrics) {
        super(context);
        this.metrics = metrics;
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

        pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                stopReset();
                swallowing = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoomTo(grid.zoom() * detector.getScaleFactor(), detector.getFocusY(), false);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                reportZoom(true);
            }
        });
        // One finger after a double tap would zoom as well, which a double tap must not do: it only resets.
        pinch.setQuickScaleEnabled(false);
        pinch.setStylusScaleEnabled(false);

        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                if (grid.zoom() > ZoomMetrics.MIN) {
                    Haptics.tap(WeekPageView.this);
                    resetZoom(event.getY());
                }
                return true;
            }
        });
        taps.setIsLongpressEnabled(false);
    }

    /** @param timetable {@code null} while loading; the grid then stays empty */
    void bind(LocalDate monday, LocalDate today, TimeWindow window, NameStyle nameStyle, Timetable timetable,
              WeekGridView.OnSessionClickListener clickListener) {
        header.bind(monday, today);
        var perDay = new ArrayList<List<SessionView>>();
        for (var i = 0; i < 5; i++) {
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

    void setZoomListener(OnZoomListener listener) {
        zoomListener = listener;
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
        scroll.post(() -> scrollWithoutReport(y.getAsInt()));
    }

    /** Sets the zoom before the page is shown; the scroll position is set separately. */
    void setZoom(float zoom) {
        grid.setZoom(zoom);
    }

    /**
     * Takes over the zoom of another page, the one being pinched, and then its scroll position. Both happen
     * after the grid has its new height, because a position beyond the old height could not be reached.
     */
    void followZoom(float zoom, IntSupplier y) {
        if (grid.zoom() == zoom) return;
        grid.setZoom(zoom);
        afterNextLayout(() -> scrollWithoutReport(y.getAsInt()));
    }

    // --- zooming ---

    /** Puts the hours back to their normal height, around the middle of the grid. */
    void resetZoom() {
        resetZoom(scroll.getTop() + scroll.getHeight() / 2f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // The grid fills the scrolling area, whose height is only known now, so a change needs a second pass.
        if (grid.setViewportHeight(scroll.getMeasuredHeight())) super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** Sees every touch before the children do, and takes the touch away from them once it becomes a pinch. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        var action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            stopReset();
            swallowing = false;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // A second finger may start a pinch, which the pager must not take for a swipe to the next week.
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        var wasSwallowing = swallowing;
        pinch.onTouchEvent(event);
        taps.onTouchEvent(event);
        if (!swallowing) return super.dispatchTouchEvent(event);

        if (!wasSwallowing) {
            // The scroll the first finger started ends here, without a fling.
            var cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
        }
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        // The page is gone, so nobody is told about the end of the animation.
        if (reset != null) {
            reset.removeAllListeners();
            reset.cancel();
            reset = null;
        }
        super.onDetachedFromWindow();
    }

    /**
     * Sets the zoom and keeps the time at {@code focusY} (measured in this page) where it is.
     */
    private void zoomTo(float requested, float focusY, boolean settled) {
        var old = grid.zoom();
        var next = ZoomMetrics.clamp(requested);
        if (next != old) {
            var focus = Math.max(0, focusY - scroll.getTop());
            float current = pendingScroll != NO_SCROLL ? pendingScroll : scroll.getScrollY();
            var target = ZoomMetrics.scrollAfterZoom(current, focus, metrics.verticalPadding, next / old);
            grid.setZoom(next);
            scrollAfterLayout(Math.round(target));
        }
        reportZoom(settled);
    }

    private void reportZoom(boolean settled) {
        if (zoomListener != null) zoomListener.onZoomed(this, grid.zoom(), settled);
    }

    private void resetZoom(float focusY) {
        if (grid.zoom() == ZoomMetrics.MIN) return;
        stopReset();
        var animator = ValueAnimator.ofFloat(grid.zoom(), ZoomMetrics.MIN);
        animator.setDuration(RESET_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> zoomTo((float) a.getAnimatedValue(), focusY, false));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                reset = null;
                reportZoom(true);
            }
        });
        reset = animator;
        animator.start();
    }

    private void stopReset() {
        if (reset != null) reset.cancel();
    }

    // --- scrolling after the grid got a new height ---

    /** The scroll of this page after a zoom step. It is reported, so the other pages follow. */
    private void scrollAfterLayout(int y) {
        pendingScroll = y;
        afterNextLayout(() -> {
            if (pendingScroll == NO_SCROLL) return;
            var target = pendingScroll;
            pendingScroll = NO_SCROLL;
            scroll.scrollTo(0, target);
        });
    }

    private void scrollWithoutReport(int y) {
        if (scroll.getScrollY() == y) return;
        following = true;
        try {
            scroll.scrollTo(0, y);
        } finally {
            following = false;
        }
    }

    /** Runs the action once, after the next layout and before the next frame is drawn. */
    private void afterNextLayout(Runnable action) {
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                var observer = getViewTreeObserver();
                if (observer.isAlive()) observer.removeOnPreDrawListener(this);
                action.run();
                return true;
            }
        });
    }
}
