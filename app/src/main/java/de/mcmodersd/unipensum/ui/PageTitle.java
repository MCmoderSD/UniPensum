package de.mcmodersd.unipensum.ui;

import static de.mcmodersd.unipensum.R.id.bar_title;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

/**
 * The title of a page that opens from the week. It is also the name of the page for a screen reader, which says it
 * when the page opens instead of leaving the user in the middle of it. The page itself only keeps touches from
 * reaching the week behind it; it is a surface and not a control, so a screen reader and the Tab key pass it by.
 */
public final class PageTitle {

    private static final AccessibilityDelegateCompat SURFACE = new AccessibilityDelegateCompat() {
        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.removeAction(AccessibilityActionCompat.ACTION_CLICK);
            info.setClickable(false);
            info.setFocusable(false);
        }
    };

    private PageTitle() { }

    public static void set(View page, CharSequence title) {
        set(page, title, title);
    }

    /** @param spoken what a screen reader says for the page, when it is more than the bar shows */
    public static void set(View page, CharSequence title, CharSequence spoken) {
        TextView bar = page.findViewById(bar_title);
        bar.setText(title);
        bar.setContentDescription(TextUtils.equals(spoken, title) ? null : spoken);
        ViewCompat.setAccessibilityPaneTitle(page, spoken);
        ViewCompat.setAccessibilityDelegate(page, SURFACE);
    }
}