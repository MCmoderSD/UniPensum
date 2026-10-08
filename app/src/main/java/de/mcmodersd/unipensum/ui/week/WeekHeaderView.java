package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/** Weekday names and dates above the grid. The current day is marked with a filled circle. */
// Created in code by WeekPageView only, never inflated from XML.
@SuppressLint("ViewConstructor")
final class WeekHeaderView extends View {

    private final GridMetrics metrics;
    private final Paint weekdayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int primary;
    private final int onAccent;

    private LocalDate monday = LocalDate.now();
    private LocalDate today = LocalDate.now();

    WeekHeaderView(Context context, GridMetrics metrics) {
        super(context);
        this.metrics = metrics;
        primary = ContextCompat.getColor(context, R.color.text_primary);
        onAccent = ContextCompat.getColor(context, R.color.text_on_accent);

        weekdayPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        weekdayPaint.setTextSize(sp(11));
        weekdayPaint.setTextAlign(Paint.Align.CENTER);
        weekdayPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        weekdayPaint.setLetterSpacing(0.06f);

        numberPaint.setTextSize(sp(16));
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        circlePaint.setColor(ContextCompat.getColor(context, R.color.accent));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    void bind(LocalDate monday, LocalDate today) {
        this.monday = monday;
        this.today = today;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), metrics.headerHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        var locale = TimeFormat.locale(getContext());
        var columnWidth = metrics.columnWidth(getWidth());
        var weekdayBaseline = getHeight() * 0.36f;
        var numberCenter = getHeight() * 0.70f;
        var radius = numberPaint.getTextSize() * 0.95f;

        for (var i = 0; i < 5; i++) {
            var day = monday.plusDays(i);
            var centerX = metrics.gutter + columnWidth * (i + 0.5f);

            var weekday = DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, locale);
            canvas.drawText(weekday.toUpperCase(locale), centerX, weekdayBaseline, weekdayPaint);

            var isToday = day.equals(today);
            if (isToday) canvas.drawCircle(centerX, numberCenter, radius, circlePaint);
            numberPaint.setColor(isToday ? onAccent : primary);
            var fm = numberPaint.getFontMetrics();
            var baseline = numberCenter - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(String.valueOf(day.getDayOfMonth()), centerX, baseline, numberPaint);
        }
    }
}