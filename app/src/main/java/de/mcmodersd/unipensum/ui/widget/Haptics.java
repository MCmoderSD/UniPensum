package de.mcmodersd.unipensum.ui.widget;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Haptic feedback in one place, so its strength can be tuned in one place. A press and the steps of a picker are the
 * same firm click, and the outcome of an action (saved, refused) has a pattern of its own.
 */
public final class Haptics {

    private Haptics() { }

    /** A plain press of a button, a row or an icon: a firm click. */
    public static void tap(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /** One step of a wheel, a segmented control, a stepper or a calendar selection: as firm as a press. */
    public static void segmentTick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
}