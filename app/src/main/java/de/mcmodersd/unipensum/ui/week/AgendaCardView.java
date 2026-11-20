package de.mcmodersd.unipensum.ui.week;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.view.ViewGroup.LayoutParams.*;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.style.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.ui.format.CourseColors;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.FocusRing;
import de.mcmodersd.unipensum.ui.widget.Haptics;

/**
 * One session in the list of a day: the tinted card with a bar in the course color that a block of the grid is,
 * as wide as the screen. Every line gets as many rows as it needs, so nothing is left out and nothing is cut off,
 * however large the text is.
 */
// Created in code by WeekAgendaView only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class AgendaCardView extends LinearLayout {

    private static final int RADIUS_DP = 14;
    private static final int BAR_DP = 4;

    private final Paint barPaint = new Paint(ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final float dp = getResources().getDisplayMetrics().density;

    AgendaCardView(Context context, SessionView session, NameStyle nameStyle) {
        super(context);
        var accent = CourseColors.resolve(context, session.color());
        barPaint.setColor(accent);

        var background = new GradientDrawable();
        background.setCornerRadius(RADIUS_DP * dp);
        background.setColor(ColorUtils.setAlphaComponent(accent, SessionBlockView.TINT_ALPHA));
        setBackground(background);
        setWillNotDraw(false);
        setOrientation(VERTICAL);
        setPadding(Math.round(20 * dp), Math.round(12 * dp), Math.round(16 * dp), Math.round(12 * dp));
        setClickable(true);
        setFocusable(true);
        FocusRing.install(this, RADIUS_DP);
        setContentDescription(SessionBlockView.describe(context, session));

        var details = session.session().details();
        var title = line(TextAppearance_Up_Body, text_primary, session.courseName());
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        line(TextAppearance_Up_Caption, text_detail, SeriesFormat.timeRange(context, details));
        line(TextAppearance_Up_Caption, text_detail, SeriesFormat.place(context, details));
        line(TextAppearance_Up_Caption, text_detail, TimeFormat.typeName(context, details.type()));
        line(
                TextAppearance_Up_Caption, text_detail,
                session.lecturer() == null ? "" : session.lecturer().name(nameStyle)
        );
    }

    /** Adds a line of text, if there is any, and returns it. */
    private TextView line(@StyleRes int appearance, int colorRes, String text) {
        var view = new TextView(getContext());
        view.setTextAppearance(appearance);
        view.setTextColor(ContextCompat.getColor(getContext(), colorRes));
        view.setText(text);
        if (text.isEmpty()) return view;
        var params = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = getChildCount() == 0 ? 0 : Math.round(2 * dp);
        addView(view, params);
        return view;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var inset = RADIUS_DP * 0.6f * dp;
        var width = BAR_DP * dp;
        barRect.set(inset * 0.5f, inset, inset * 0.5f + width, getHeight() - inset);
        canvas.drawRoundRect(barRect, width / 2f, width / 2f, barPaint);
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
}