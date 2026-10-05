package de.mcmodersd.unipensum.ui.widget;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Haptic feedback with the finer constants of Android 14 where they exist and the older,
 * coarser ones as a replacement on Android 12 and 13.
 */
public final class Haptics {

    private Haptics() {
    }

    /** A plain press. */
    public static void tap(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    /** A completed action such as saving. */
    public static void confirm(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    /** An action that was refused, such as an invalid form. */
    public static void reject(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT);
    }

    public static void toggle(View view, boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(on ? HapticFeedbackConstants.TOGGLE_ON : HapticFeedbackConstants.TOGGLE_OFF);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    /** One step of a discrete picker, a segmented control or a calendar selection. */
    public static void segmentTick(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }
}
