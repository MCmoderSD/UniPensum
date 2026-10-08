package de.mcmodersd.unipensum.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.R;

/**
 * The frame of the app's main screen. It holds two containers: the first is the week calendar, the
 * second is the pane for the pages that open from it (settings, courses, editors).
 * <ul>
 *   <li>In a wide window (see {@link PaneMetrics}) the pane slides in from the right and the calendar
 *       gives up that much width, so both stay visible and usable.</li>
 *   <li>In a narrow window the pane covers the calendar completely, as a page of its own. It animates
 *       through the pages' own transitions, and the covered calendar is hidden from screen readers and
 *       the keyboard focus.</li>
 * </ul>
 * The pane container is always there, empty and not clickable when no page is open, so touches pass through
 * it to the calendar. It is never hidden: a page that is animating out must stay drawn until it is gone.
 * Whether the pane is open is told by {@link #setSideOpen}; the layout does not know about fragments.
 */
public final class PaneLayout extends ViewGroup {

    /** The pane's share of the window width, within the bounds of the dimens. */
    private static final float SIDE_SHARE = 0.4f;
    private static final long OPEN_MILLIS = 260;
    private static final long CLOSE_MILLIS = 220;

    private final int splitMinWidth;
    private final int sideMinWidth;
    private final int sideMaxWidth;
    private final int dividerWidth;
    private final Paint dividerPaint = new Paint();

    private ViewGroup main;
    private ViewGroup side;
    /** How far the pane has slid in, 0 to 1. Only used in a wide window. */
    private float progress;
    private boolean split;
    private ValueAnimator animator;

    public PaneLayout(Context context) {
        this(context, null);
    }

    public PaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        splitMinWidth = getResources().getDimensionPixelSize(R.dimen.pane_split_min_width);
        sideMinWidth = getResources().getDimensionPixelSize(R.dimen.pane_side_min);
        sideMaxWidth = getResources().getDimensionPixelSize(R.dimen.pane_side_max);
        dividerWidth = Math.max(1, Math.round(getResources().getDisplayMetrics().density));
        dividerPaint.setColor(ContextCompat.getColor(context, R.color.divider));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalStateException("PaneLayout needs exactly two children: the main and the side container");
        }
        main = (ViewGroup) getChildAt(0);
        side = (ViewGroup) getChildAt(1);
    }

    /** Whether the window is wide enough for the calendar and the pane to sit next to each other. */
    public boolean isSplit() {
        var density = getResources().getDisplayMetrics().density;
        var windowWidth = Math.round(getResources().getConfiguration().screenWidthDp * density);
        return PaneMetrics.isSplit(windowWidth, splitMinWidth);
    }

    /**
     * Opens or closes the pane.
     *
     * @param animate slide it, unless the layout has not been shown yet (after the screen was recreated)
     */
    public void setSideOpen(boolean open, boolean animate) {
        var covered = open && !isSplit();
        main.setImportantForAccessibility(
                covered
                        ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : IMPORTANT_FOR_ACCESSIBILITY_AUTO
        );
        main.setDescendantFocusability(covered ? FOCUS_BLOCK_DESCENDANTS : FOCUS_BEFORE_DESCENDANTS);

        var target = open ? 1f : 0f;
        if (animator != null) animator.cancel();
        if (!animate || !isLaidOut() || !isSplit()) {
            progress = target;
            requestLayout();
            invalidate();
            return;
        }
        if (progress == target) return;

        var slide = ValueAnimator.ofFloat(progress, target);
        slide.setDuration(open ? OPEN_MILLIS : CLOSE_MILLIS);
        slide.setInterpolator(
                AnimationUtils.loadInterpolator(
                        getContext(),
                        open ? android.R.interpolator.decelerate_cubic : android.R.interpolator.accelerate_cubic
                )
        );
        slide.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            requestLayout();
            invalidate();
        });
        slide.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator finished) {
                if (animator == finished) animator = null;
            }
        });
        animator = slide;
        slide.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        var width = MeasureSpec.getSize(widthMeasureSpec);
        var height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        var innerWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        var innerHeight = Math.max(0, height - getPaddingTop() - getPaddingBottom());
        split = isSplit();
        var mainWidth = innerWidth;
        var sideWidth = innerWidth;
        if (split) {
            // The pane keeps its final width while it slides, so its page is laid out once and not on every frame.
            sideWidth = PaneMetrics.sideWidth(innerWidth, SIDE_SHARE, sideMinWidth, sideMaxWidth);
            mainWidth = innerWidth - Math.round(sideWidth * progress);
        }
        measureExactly(main, mainWidth, innerHeight);
        measureExactly(side, sideWidth, innerHeight);
    }

    private static void measureExactly(View child, int width, int height) {
        child.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        var innerLeft = getPaddingLeft();
        var innerTop = getPaddingTop();
        var innerRight = right - left - getPaddingRight();

        main.layout(innerLeft, innerTop, innerLeft + main.getMeasuredWidth(), innerTop + main.getMeasuredHeight());
        // Closed, the pane lies just outside the right edge and is clipped away.
        var sideLeft = split ? innerRight - Math.round(side.getMeasuredWidth() * progress) : innerLeft;
        side.layout(sideLeft, innerTop, sideLeft + side.getMeasuredWidth(), innerTop + side.getMeasuredHeight());
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!split || progress <= 0f) return;
        // A thin line where the calendar ends and the pane begins.
        canvas.drawRect(
                side.getLeft() - dividerWidth, getPaddingTop(), side.getLeft(),
                getHeight() - getPaddingBottom(), dividerPaint
        );
    }
}
