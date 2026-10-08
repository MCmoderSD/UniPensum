package de.mcmodersd.unipensum.ui.week;

import android.content.Context;
import android.content.res.Resources;

import de.mcmodersd.unipensum.R;

/**
 * Every measurement of the week grid in one place. Open question 1 of the requirements (whole day on
 * one screen versus scrolling) is a matter of {@link #minHourHeight}: the grid fills the screen as long
 * as an hour gets at least that much room and scrolls below it.
 */
final class GridMetrics {

    final int gutter;
    final int minHourHeight;
    final int verticalPadding;
    final int blockInset;
    final int accentBar;
    final int nowLine;
    final int headerHeight;

    GridMetrics(Context context) {
        var res = context.getResources();
        gutter = res.getDimensionPixelSize(R.dimen.grid_gutter);
        minHourHeight = res.getDimensionPixelSize(R.dimen.grid_min_hour_height);
        verticalPadding = res.getDimensionPixelSize(R.dimen.grid_vertical_padding);
        blockInset = res.getDimensionPixelSize(R.dimen.grid_block_inset);
        accentBar = res.getDimensionPixelSize(R.dimen.grid_accent_bar);
        nowLine = res.getDimensionPixelSize(R.dimen.grid_now_line);
        headerHeight = res.getDimensionPixelSize(R.dimen.week_header_height);
    }

    float columnWidth(int totalWidth) {
        return (totalWidth - gutter) / 5f;
    }
}
