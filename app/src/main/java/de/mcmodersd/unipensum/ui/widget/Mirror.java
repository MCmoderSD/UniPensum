package de.mcmodersd.unipensum.ui.widget;

import static android.view.View.LAYOUT_DIRECTION_RTL;

import android.view.View;

/**
 * For views that draw their own columns: whether the layout runs from right to left, in which case the first column
 * is on the right. Views built from layouts need no help, they mirror themselves.
 */
public final class Mirror {

    private Mirror() { }

    public static boolean isRtl(View view) {
        return view.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }
}