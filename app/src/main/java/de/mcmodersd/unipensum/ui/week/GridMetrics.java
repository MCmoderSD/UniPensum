package de.mcmodersd.unipensum.ui.week;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static de.mcmodersd.unipensum.R.dimen.*;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;

import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * Every measurement of the week grid in one place. Open question 1 of the requirements (whole day on
 * one screen versus scrolling) is a matter of {@link #minHourHeight}: the grid fills the screen as long
 * as an hour gets at least that much room and scrolls below it.
 * <p>
 * The hour labels and the day header are text, so they grow with the font size of the system: the gutter is as
 * wide as the widest label needs, and the header as tall as the day numbers need. Both are never smaller than the
 * dimensions of {@code dimens.xml}.
 */
final class GridMetrics {

    /** The size of the hour labels, and the gap between them and the grid, which the grid draws with. */
    static final float HOUR_LABEL_SP = 10;
    static final float HOUR_LABEL_GAP_DP = 6;

    private static final float HOUR_LABEL_MARGIN_DP = 2;
    private static final float DAY_NUMBER_SP = 16;

    final int gutter;
    final int minHourHeight;
    final int verticalPadding;
    final int blockInset;
    final int accentBar;
    final int nowLine;
    final int headerHeight;

    GridMetrics(Context context) {
        var res = context.getResources();
        var dp = res.getDisplayMetrics().density;
        gutter = Math.max(
                res.getDimensionPixelSize(grid_gutter),
                Math.round(widestHourLabel(context, res) + (HOUR_LABEL_GAP_DP + HOUR_LABEL_MARGIN_DP) * dp)
        );
        minHourHeight = res.getDimensionPixelSize(grid_min_hour_height);
        verticalPadding = res.getDimensionPixelSize(grid_vertical_padding);
        blockInset = res.getDimensionPixelSize(grid_block_inset);
        accentBar = res.getDimensionPixelSize(grid_accent_bar);
        nowLine = res.getDimensionPixelSize(grid_now_line);
        headerHeight = Math.round(res.getDimensionPixelSize(week_header_height) * textScale(res));
    }

    float columnWidth(int totalWidth) {
        return (totalWidth - gutter) / 5f;
    }

    /** How much the font size of the system has stretched the text: 1 at the normal size, never less. */
    private static float textScale(Resources res) {
        var metrics = res.getDisplayMetrics();
        var scaled = TypedValue.applyDimension(COMPLEX_UNIT_SP, DAY_NUMBER_SP, metrics);
        return Math.max(1f, scaled / (DAY_NUMBER_SP * metrics.density));
    }

    /** The width of the widest label of the time axis, in the weight the label of the current hour has. */
    private static float widestHourLabel(Context context, Resources res) {
        var paint = new Paint();
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(TypedValue.applyDimension(COMPLEX_UNIT_SP, HOUR_LABEL_SP, res.getDisplayMetrics()));
        var widest = 0f;
        for (var hour = 0; hour < 24; hour++) {
            widest = Math.max(widest, paint.measureText(TimeFormat.hour(context, hour)));
        }
        return widest;
    }
}