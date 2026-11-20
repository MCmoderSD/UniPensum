package de.mcmodersd.unipensum.ui;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.mcmodersd.unipensum.R;

/** A page that opens from the week names itself for a screen reader, and is not a control to stop at. */
@RunWith(AndroidJUnit4.class)
public class PageTitleTest {

    private final Context context = new ContextThemeWrapper(
            ApplicationProvider.<Context>getApplicationContext(), R.style.Theme_UniPensum
    );

    /** A page as the layouts build it: clickable, so that touches do not reach the week behind it. */
    private FrameLayout page() {
        var page = new FrameLayout(context);
        page.setClickable(true);
        page.setFocusable(true);
        LayoutInflater.from(context).inflate(R.layout.view_top_bar, page, true);
        return page;
    }

    private static TextView bar(FrameLayout page) {
        return page.findViewById(R.id.bar_title);
    }

    @Test
    public void theTitleIsTheNameOfThePage() {
        var page = page();
        PageTitle.set(page, "Settings");
        assertEquals("Settings", bar(page).getText().toString());
        assertEquals("Settings", ViewCompat.getAccessibilityPaneTitle(page).toString());
        assertNull(bar(page).getContentDescription());
    }

    @Test
    public void aScreenReaderMayHearMoreThanTheBarShows() {
        var page = page();
        PageTitle.set(page, "Winter 26/27", "Courses, Winter 26/27");
        assertEquals("Winter 26/27", bar(page).getText().toString());
        assertEquals("Courses, Winter 26/27", bar(page).getContentDescription().toString());
        assertEquals("Courses, Winter 26/27", ViewCompat.getAccessibilityPaneTitle(page).toString());
    }

    @Test
    public void thePageIsASurfaceAndNotAControl() {
        var page = page();
        PageTitle.set(page, "Settings");
        // It still keeps the touches, but a screen reader and the Tab key pass it by.
        assertTrue(page.isClickable());
        var node = AccessibilityNodeInfoCompat.wrap(page.createAccessibilityNodeInfo());
        assertFalse(node.isClickable());
        assertFalse(node.isFocusable());
    }
}