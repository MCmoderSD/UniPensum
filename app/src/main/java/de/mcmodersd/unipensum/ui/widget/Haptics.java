package de.mcmodersd.unipensum.ui.widget;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
import static android.view.HapticFeedbackConstants.*;

import android.view.View;

/**
 * Haptic feedback in one place, so its strength can be tuned in one place. A press and the steps of a picker are the
 * same firm click, and the outcome of an action (saved, refused) has a pattern of its own.
 */
public final class Haptics {

    private Haptics() { }

    /** A plain press of a button, a row or an icon: a firm click. */
    public static void tap(View view) {
        view.performHapticFeedback(VIRTUAL_KEY);
    }

    /** A completed action such as saving. */
    public static void confirm(View view) {
        view.performHapticFeedback(CONFIRM);
    }

    /** An action that was refused, such as an invalid form. */
    public static void reject(View view) {
        view.performHapticFeedback(REJECT);
    }

    public static void toggle(View view, boolean on) {
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(on ? TOGGLE_ON : TOGGLE_OFF);
        } else {
            view.performHapticFeedback(VIRTUAL_KEY);
        }
    }

    /** One step of a wheel, a segmented control, a stepper or a calendar selection: as firm as a press. */
    public static void segmentTick(View view) {
        view.performHapticFeedback(VIRTUAL_KEY);
    }
}