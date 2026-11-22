package de.mcmodersd.unipensum.ui.widget;

import static android.R.attr.state_focused;
import static de.mcmodersd.unipensum.R.color.accent;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * The mark of the keyboard focus on every control: a ring in the accent color, as round as the control, in place of
 * the gray square the system draws. It shows for keyboard, D-pad and switch focus only, because the controls do not
 * take the focus from a touch.
 */
public final class FocusRing {

    private static final float WIDTH_DP = 2;

    private FocusRing() { }

    /** A ring along the edge of the whole view. */
    public static void install(View view, float radiusDp) {
        install(view, radiusDp, 0);
    }

    /**
     * @param insetDp how far the ring sits inside the view on top and bottom, for a control whose touch target is
     *                taller than what it draws
     */
    public static void install(View view, float radiusDp, float insetDp) {
        var dp = view.getResources().getDisplayMetrics().density;
        var ring = new GradientDrawable();
        ring.setCornerRadius(radiusDp * dp);
        ring.setStroke(Math.round(WIDTH_DP * dp), ContextCompat.getColor(view.getContext(), accent));
        var states = new StateListDrawable();
        states.addState(new int[]{state_focused}, ring);
        var inset = Math.round(insetDp * dp);
        view.setForeground(new InsetDrawable(states, 0, inset, 0, inset));
        view.setDefaultFocusHighlightEnabled(false);
    }
}