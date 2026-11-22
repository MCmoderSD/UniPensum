package de.mcmodersd.unipensum.ui.widget;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Shows a message that a screen reader says when it appears, as the answer to a press on Save. A screen reader does not
 * say the same text of a live region twice in a row, so a message that is shown again is cleared first and set again in
 * the next frame: the second press on Save says "Enter a name." as well as the first. Switching the view off and on does
 * not help, the text has to change.
 */
public final class LiveMessage {

    /** Two messages set in the same frame reach a screen reader as one change, of which it says only one. */
    private static final long STAGGER_MILLIS = 150;
    /** When the next message that is shown again may be set, so that every one of them is a change of its own. */
    private static long free;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView view;
    private Runnable pending;

    public LiveMessage(TextView view) {
        this.view = view;
    }

    /** Shows the message, or hides the view and forgets the text for {@code null}. */
    public void show(@Nullable CharSequence message) {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
        if (message == null) {
            view.setText(null);
            view.setVisibility(GONE);
            return;
        }
        var again = view.getVisibility() == VISIBLE && TextUtils.equals(view.getText(), message);
        view.setVisibility(VISIBLE);
        if (!again) {
            view.setText(message);
            return;
        }
        view.setText("");
        pending = () -> {
            pending = null;
            view.setText(message);
        };
        var at = Math.max(SystemClock.uptimeMillis(), free);
        free = at + STAGGER_MILLIS;
        handler.postAtTime(pending, at);
    }
}