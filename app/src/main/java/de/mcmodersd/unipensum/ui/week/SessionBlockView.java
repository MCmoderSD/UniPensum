package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.text.LineBreaker;
import android.text.Layout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.ui.format.CourseColors;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.Haptics;

/**
 * One session in the grid: a tinted card with an accent bar in the course color. Overlapping
 * sessions get a dashed outline. The block shows as much as its size allows, in this order:
 * course, time, place, type, lecturer. Long values wrap instead of being cut off.
 */
// Created in code by WeekGridView only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class SessionBlockView extends LinearLayout {

    /** Lets the time range break after the dash when it does not fit on one line. */
    private static final String BREAKABLE_DASH = "–​";

    private final SessionView session;
    private final TextView title;
    private final TextView time;
    private final TextView place;
    private final TextView type;
    private final TextView lecturer;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final int barWidth;
    private final float radius;
    private final float dp;
    private int fittedWidth = -1;
    private int fittedHeight = -1;

    SessionBlockView(Context context, GridMetrics metrics, SessionView session, NameStyle nameStyle,
                     boolean overlapping) {
        super(context);
        this.session = session;
        this.barWidth = metrics.accentBar;
        this.radius = getResources().getDimension(R.dimen.radius_block);
        this.dp = getResources().getDisplayMetrics().density;

        var accent = CourseColors.resolve(context, session.color());
        barPaint.setColor(accent);

        var background = new GradientDrawable();
        background.setCornerRadius(radius);
        background.setColor(ColorUtils.setAlphaComponent(accent, 0x2E));
        if (overlapping) {
            background.setStroke(Math.round(1.5f * dp), accent, 4 * dp, 3 * dp);
        }
        setBackground(background);
        setWillNotDraw(false);
        setOrientation(VERTICAL);
        setGravity(Gravity.TOP);
        setClickable(true);
        setFocusable(true);

        var details = session.session().details();
        title = line(context, true, session.courseName());
        // Two sessions side by side leave very little width; hyphenate instead of breaking inside a word.
        title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        title.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        time = line(
                context, false, TimeFormat.time(context, details.startMin()) + BREAKABLE_DASH
                        + TimeFormat.time(context, details.endMin())
        );
        place = line(context, false, SeriesFormat.place(context, details));
        type = line(context, false, TimeFormat.typeName(context, details.type()));
        lecturer = line(context, false, session.lecturer() == null ? "" : session.lecturer().name(nameStyle));

        setContentDescription(
                context.getString(
                        R.string.session_description,
                        session.courseName(),
                        TimeFormat.typeName(context, details.type()),
                        TimeFormat.time(context, details.startMin()),
                        TimeFormat.time(context, details.endMin())
                )
        );
    }

    SessionView session() {
        return session;
    }

    /**
     * Decides what fits into the final size: every line gets as many rows as it needs, up to a limit,
     * and whatever no longer fits is dropped from the bottom of the priority list. Call while the
     * parent measures; the result is cached, so repeated passes with the same size do nothing.
     */
    void fitTo(int widthPx, int heightPx) {
        if (widthPx == fittedWidth && heightPx == fittedHeight) return;
        fittedWidth = widthPx;
        fittedHeight = heightPx;

        var narrow = widthPx < 52 * dp;
        setPadding(
                barWidth + Math.round((narrow ? 2 : 4) * dp), Math.round(3 * dp),
                Math.round((narrow ? 1 : 3) * dp), Math.round(3 * dp)
        );
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, narrow ? 9.5f : 11.5f);
        for (var detail : new TextView[]{time, place, type, lecturer}) {
            detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, narrow ? 9f : 10f);
        }

        var contentWidth = Math.max(0, widthPx - getPaddingLeft() - getPaddingRight());
        var budget = Math.max(0, heightPx - getPaddingTop() - getPaddingBottom());
        budget -= fit(title, contentWidth, budget, 3, true);
        budget -= fit(time, contentWidth, budget, 2, false);
        budget -= fit(place, contentWidth, budget, 3, false);
        budget -= fit(type, contentWidth, budget, 1, false);
        fit(lecturer, contentWidth, budget, 2, false);
    }

    /**
     * Shows the line with the most rows (up to {@code maxLines}) that still fit the remaining height.
     *
     * @param required keep at least one row even if the block is too small, as for the course name
     * @return the height the line takes, 0 if it was hidden
     */
    private static int fit(TextView view, int width, int budget, int maxLines, boolean required) {
        if (view.length() == 0) {
            view.setVisibility(GONE);
            return 0;
        }
        var widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        var heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        for (var lines = maxLines; lines >= 1; lines--) {
            view.setMaxLines(lines);
            view.measure(widthSpec, heightSpec);
            if (view.getMeasuredHeight() <= budget || (required && lines == 1)) {
                view.setVisibility(VISIBLE);
                return view.getMeasuredHeight();
            }
        }
        view.setVisibility(GONE);
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var inset = radius * 0.6f;
        barRect.set(inset * 0.5f, inset, inset * 0.5f + barWidth, getHeight() - inset);
        canvas.drawRoundRect(barRect, barWidth / 2f, barWidth / 2f, barPaint);
    }

    @Override
    public boolean performClick() {
        Haptics.tap(this);
        return super.performClick();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setAlpha(pressed ? 0.65f : 1f);
    }

    private TextView line(Context context, boolean primary, String text) {
        var view = new TextView(context);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(context, primary ? R.color.text_primary : R.color.text_secondary));
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(primary ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return view;
    }
}
