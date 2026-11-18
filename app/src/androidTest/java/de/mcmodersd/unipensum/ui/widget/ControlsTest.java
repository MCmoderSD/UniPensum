package de.mcmodersd.unipensum.ui.widget;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.mcmodersd.unipensum.R;

/** The controls give a finger the 48dp Android asks for, and tell a screen reader what they are. */
@RunWith(AndroidJUnit4.class)
public class ControlsTest {

    private final Context context = new ContextThemeWrapper(
            ApplicationProvider.<Context>getApplicationContext(), R.style.Theme_UniPensum
    );
    private final float dp = context.getResources().getDisplayMetrics().density;

    private void measure(View view) {
        view.measure(makeMeasureSpec(2000, AT_MOST), makeMeasureSpec(2000, AT_MOST));
    }

    @Test
    public void iconButtonIs48dp() {
        var button = new UpIconButton(context);
        measure(button);
        assertEquals(Math.round(48 * dp), button.getMeasuredWidth());
        assertEquals(Math.round(48 * dp), button.getMeasuredHeight());
    }

    @Test
    public void switchTouchTargetIs48dpTall() {
        var toggle = new UpSwitch(context);
        measure(toggle);
        assertEquals(Math.round(48 * dp), toggle.getMeasuredHeight());
        assertTrue(toggle.getMeasuredWidth() >= Math.round(48 * dp));
    }

    @Test
    public void segmentsAre48dpTall() {
        var control = new UpSegmentedControl(context);
        control.setOptions("Lecture", "Exercise", "Lab");
        measure(control);
        assertEquals(3, control.getChildCount());
        for (var i = 0; i < control.getChildCount(); i++) {
            assertTrue(control.getChildAt(i).getMeasuredHeight() >= Math.round(48 * dp));
        }
    }

    @Test
    public void tappingTheRowTogglesTheSwitch() {
        var row = new LinearLayout(context);
        var toggle = new UpSwitch(context);
        row.addView(toggle);
        UpSwitch.bindRow(row, toggle);

        assertFalse(toggle.isChecked());
        // The switch animates, which needs the main thread.
        getInstrumentation().runOnMainSync(row::performClick);
        assertTrue(toggle.isChecked());
        getInstrumentation().runOnMainSync(row::performClick);
        assertFalse(toggle.isChecked());
    }

    @Test
    public void theRowOfASwitchIsNotReadAgainByAScreenReader() {
        var row = new LinearLayout(context);
        UpSwitch.bindRow(row, new UpSwitch(context));
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, row.getImportantForAccessibility());
    }

    @Test
    public void segmentedControlIsAGroupOfRadioButtons() {
        var control = new UpSegmentedControl(context);
        control.setOptions("System", "Light", "Dark");
        control.setSelectedIndex(1);

        var group = AccessibilityNodeInfoCompat.wrap(control.createAccessibilityNodeInfo());
        assertEquals(RadioGroup.class.getName(), group.getClassName().toString());
        assertEquals(3, group.getCollectionInfo().getColumnCount());
        assertEquals(
                AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE,
                group.getCollectionInfo().getSelectionMode()
        );

        for (var i = 0; i < 3; i++) {
            var node = AccessibilityNodeInfoCompat.wrap(control.getChildAt(i).createAccessibilityNodeInfo());
            assertEquals(RadioButton.class.getName(), node.getClassName().toString());
            assertTrue(node.isCheckable());
            assertEquals(i == 1, node.isChecked());
            assertEquals(i, node.getCollectionItemInfo().getColumnIndex());
        }
    }

    @Test
    public void focusShowsAnAccentRingInsteadOfTheSystemHighlight() {
        var button = new UpButton(context);
        assertNotNull(button.getForeground());
        assertFalse(button.getDefaultFocusHighlightEnabled());
    }
}