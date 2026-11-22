package de.mcmodersd.unipensum.ui.widget;

import static android.view.KeyEvent.*;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.CourseColor;

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

    // --- names, messages and roles for a screen reader ---

    @Test
    public void aFieldIsNamedByItsLabel() {
        var field = new UpTextField(context);
        field.setLabel("Name");
        var label = (TextView) field.getChildAt(0);
        var input = field.getChildAt(1);
        assertNotEquals(View.NO_ID, input.getId());
        assertEquals(input.getId(), label.getLabelFor());
    }

    @Test
    public void aRefusedFieldCarriesItsMessage() {
        var field = new UpTextField(context);
        var input = field.getChildAt(1);
        assertFalse(AccessibilityNodeInfoCompat.wrap(input.createAccessibilityNodeInfo()).isContentInvalid());

        field.setError("Enter a name.");
        var refused = AccessibilityNodeInfoCompat.wrap(input.createAccessibilityNodeInfo());
        assertTrue(refused.isContentInvalid());
        assertEquals("Enter a name.", refused.getError().toString());

        field.setError(null);
        assertFalse(AccessibilityNodeInfoCompat.wrap(input.createAccessibilityNodeInfo()).isContentInvalid());
    }

    @Test
    public void aMessageIsSaidWhenItShows() {
        var field = new UpTextField(context);
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, field.getChildAt(2).getAccessibilityLiveRegion());
    }

    @Test
    public void aChoiceIsNotAStateOfTheViewThatWakesAScreenReader() {
        var control = new UpSegmentedControl(context);
        control.setOptions("System", "Light", "Dark");
        control.setSelectedIndex(1);
        for (var i = 0; i < control.getChildCount(); i++) {
            assertFalse(control.getChildAt(i).isSelected());
        }
    }

    @Test
    public void everyOptionOfANamedGroupSaysTheName() {
        var control = new UpSegmentedControl(context);
        control.setLabel("Type");
        control.setOptions("Lecture", "Exercise");
        var node = AccessibilityNodeInfoCompat.wrap(control.getChildAt(0).createAccessibilityNodeInfo());
        assertEquals(context.getString(R.string.option_description, "Type", "Lecture"), node.getContentDescription().toString());
    }

    @Test
    public void optionsThatDoNotFitStandInAColumn() {
        var narrow = new UpSegmentedControl(context);
        narrow.setOptions("Lecture", "Exercise", "Laboratory", "Tutorial");
        narrow.measure(makeMeasureSpec(Math.round(200 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        assertEquals(LinearLayout.VERTICAL, narrow.getOrientation());
        assertEquals(4, AccessibilityNodeInfoCompat.wrap(narrow.createAccessibilityNodeInfo()).getCollectionInfo().getRowCount());
        for (var i = 0; i < narrow.getChildCount(); i++) {
            assertTrue(narrow.getChildAt(i).getMeasuredHeight() >= Math.round(48 * dp));
            assertTrue(narrow.getChildAt(i).getMeasuredWidth() > Math.round(150 * dp));
        }

        var wide = new UpSegmentedControl(context);
        wide.setOptions("Lecture", "Exercise", "Laboratory", "Tutorial");
        wide.measure(makeMeasureSpec(Math.round(1200 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        assertEquals(LinearLayout.HORIZONTAL, wide.getOrientation());
    }

    @Test
    public void aNamedStepperNamesItsButtons() {
        var stepper = new UpStepper(context);
        stepper.setLabel("Day starts at");
        assertEquals(
                context.getString(R.string.stepper_decrease_named, "Day starts at"),
                stepper.getChildAt(0).getContentDescription().toString()
        );
        assertEquals(
                context.getString(R.string.stepper_increase_named, "Day starts at"),
                stepper.getChildAt(2).getContentDescription().toString()
        );
    }

    @Test
    public void aButtonSaysButton() {
        assertEquals(Button.class.getName(), new UpButton(context).getAccessibilityClassName());
    }

    @Test
    public void aRowIsOneThingAndAButtonOnlyWhereItCanBeTapped() {
        var row = new UpRow(context);
        assertTrue(ViewCompat.isScreenReaderFocusable(row));
        assertNotEquals(Button.class.getName(), row.getAccessibilityClassName());
        row.setOnClickListener(v -> { });
        assertEquals(Button.class.getName(), row.getAccessibilityClassName());
    }

    @Test
    public void aValueMovesUnderTheTextsWhenThereIsNoRoomBesideThem() {
        var row = new UpRow(context);
        row.setTitle("UniPensum");
        row.setValue("Version 1.0-SNAPSHOT");
        var texts = (ViewGroup) row.getChildAt(0);

        row.measure(makeMeasureSpec(Math.round(1000 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        // the title and the subtitle; the value is a child of the row
        assertEquals(2, texts.getChildCount());

        row.measure(makeMeasureSpec(Math.round(150 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        assertEquals(3, texts.getChildCount());

        row.measure(makeMeasureSpec(Math.round(1000 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        assertEquals(2, texts.getChildCount());
    }

    // --- the keyboard ---

    private static void press(View view, int key) {
        view.dispatchKeyEvent(new KeyEvent(ACTION_DOWN, key));
        view.dispatchKeyEvent(new KeyEvent(ACTION_UP, key));
    }

    @Test
    public void theArrowKeysAndEnterChooseADayOfTheCalendar() {
        var calendar = new MonthCalendarView(context);
        calendar.setSelected(LocalDate.of(2026, 10, 14));
        var chosen = new ArrayList<LocalDate>();
        calendar.setOnDateSelectedListener(chosen::add);
        calendar.measure(makeMeasureSpec(Math.round(400 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        calendar.layout(0, 0, calendar.getMeasuredWidth(), calendar.getMeasuredHeight());

        var grid = calendar.getChildAt(1);
        assertTrue(grid.isFocusable());
        // A window is in touch mode until a key is pressed, and then the focus can be given at all.
        grid.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            // The focus starts on the chosen day, and one arrow key moves it on by a day.
            grid.requestFocus();
            press(grid, KEYCODE_DPAD_RIGHT);
            press(grid, KEYCODE_ENTER);
        });
        assertEquals(List.of(LocalDate.of(2026, 10, 15)), chosen);
    }

    @Test
    public void theArrowKeysAndEnterChooseAColor() {
        var picker = new ColorSwatchPicker(context);
        picker.setSelectedColor(CourseColor.BLUE);
        var chosen = new ArrayList<CourseColor>();
        picker.setOnColorSelectedListener(chosen::add);
        picker.measure(makeMeasureSpec(Math.round(400 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        picker.layout(0, 0, picker.getMeasuredWidth(), picker.getMeasuredHeight());

        assertTrue(picker.isFocusable());
        picker.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            picker.requestFocus();
            press(picker, KEYCODE_DPAD_LEFT);
            press(picker, KEYCODE_ENTER);
        });
        assertEquals(List.of(CourseColor.TEAL), chosen);
    }

    @Test
    public void tabLeavesTheCalendarInsteadOfWalkingItsDays() {
        var calendar = new MonthCalendarView(context);
        calendar.setSelected(LocalDate.of(2026, 10, 14));
        var chosen = new ArrayList<LocalDate>();
        calendar.setOnDateSelectedListener(chosen::add);
        calendar.measure(makeMeasureSpec(Math.round(400 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        calendar.layout(0, 0, calendar.getMeasuredWidth(), calendar.getMeasuredHeight());

        var grid = calendar.getChildAt(1);
        grid.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            grid.requestFocus();
            // Not taken by the grid, so the focus goes on to the next control, and the focus in the grid stays.
            assertFalse(grid.dispatchKeyEvent(new KeyEvent(ACTION_DOWN, KEYCODE_TAB)));
            press(grid, KEYCODE_ENTER);
        });
        assertEquals(List.of(LocalDate.of(2026, 10, 14)), chosen);
    }

    @Test
    public void tabLeavesTheColorsInsteadOfWalkingThem() {
        var picker = new ColorSwatchPicker(context);
        picker.setSelectedColor(CourseColor.BLUE);
        var chosen = new ArrayList<CourseColor>();
        picker.setOnColorSelectedListener(chosen::add);
        picker.measure(makeMeasureSpec(Math.round(400 * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        picker.layout(0, 0, picker.getMeasuredWidth(), picker.getMeasuredHeight());

        picker.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            picker.requestFocus();
            assertFalse(picker.dispatchKeyEvent(new KeyEvent(ACTION_DOWN, KEYCODE_TAB)));
            // The focus is still on the chosen color; one more arrow key moves it by one.
            press(picker, KEYCODE_DPAD_LEFT);
            press(picker, KEYCODE_ENTER);
        });
        assertEquals(List.of(CourseColor.TEAL), chosen);
    }

    // --- right to left ---

    private void layOut(View view, int widthDp) {
        view.measure(makeMeasureSpec(Math.round(widthDp * dp), EXACTLY), makeMeasureSpec(4000, AT_MOST));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private void tap(View view, float x, float y) {
        var down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
        var up = MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, x, y, 0);
        getInstrumentation().runOnMainSync(() -> {
            view.dispatchTouchEvent(down);
            view.dispatchTouchEvent(up);
        });
        down.recycle();
        up.recycle();
    }

    @Test
    public void textStartsAtTheStartOfItsViewAndSegmentsAndValuesStayInTheMiddle() {
        // The alignment of a view is only resolved once it is measured.
        var text = new TextView(context);
        measure(text);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_START, text.getTextAlignment());

        var control = new UpSegmentedControl(context);
        control.setOptions("System", "Light");
        measure(control);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, control.getChildAt(0).getTextAlignment());
        var stepper = new UpStepper(context);
        measure(stepper);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, stepper.getChildAt(1).getTextAlignment());
        var button = new UpButton(context);
        measure(button);
        assertEquals(View.TEXT_ALIGNMENT_CENTER, button.getTextAlignment());
    }

    @Test
    public void theValueOfARowKeepsItsGapOnTheSideOfTheTexts() {
        var row = new UpRow(context);
        row.setTitle("Notifications");
        row.setValue("On");
        var texts = row.getChildAt(0);
        var value = row.getChildAt(1);

        layOut(row, 400);
        assertTrue(texts.getRight() <= value.getLeft());
        assertEquals(Math.round(12 * dp), value.getPaddingLeft());

        row.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(row, 400);
        assertTrue(value.getRight() <= texts.getLeft());
        assertEquals(Math.round(12 * dp), value.getPaddingRight());
    }

    private int[] colorsAtTheEndsOf(UpSwitch toggle) {
        layOut(toggle, 100);
        var bitmap = Bitmap.createBitmap(toggle.getWidth(), toggle.getHeight(), Bitmap.Config.ARGB_8888);
        toggle.draw(new Canvas(bitmap));
        var y = toggle.getHeight() / 2;
        return new int[]{
                bitmap.getPixel(Math.round(16 * dp), y),
                bitmap.getPixel(toggle.getWidth() - Math.round(16 * dp), y)
        };
    }

    @Test
    public void theThumbOfASwitchThatIsOnSitsAtTheEndOfTheTrack() {
        var track = ContextCompat.getColor(context, R.color.accent);
        var thumb = ContextCompat.getColor(context, R.color.text_on_accent);
        var toggle = new UpSwitch(context);
        toggle.setChecked(true);

        var leftToRight = colorsAtTheEndsOf(toggle);
        assertEquals(track, leftToRight[0]);
        assertEquals(thumb, leftToRight[1]);

        toggle.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        var rightToLeft = colorsAtTheEndsOf(toggle);
        assertEquals(thumb, rightToLeft[0]);
        assertEquals(track, rightToLeft[1]);
    }

    @Test
    public void theFirstColorIsOnTheRightWhenTheLayoutRunsRightToLeft() {
        var picker = new ColorSwatchPicker(context);
        var chosen = new ArrayList<CourseColor>();
        picker.setOnColorSelectedListener(chosen::add);

        picker.setSelectedColor(CourseColor.BLUE);
        layOut(picker, 400);
        tap(picker, 10 * dp, 10 * dp);

        picker.setSelectedColor(CourseColor.BLUE);
        picker.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(picker, 400);
        tap(picker, picker.getWidth() - 10 * dp, 10 * dp);

        assertEquals(List.of(CourseColor.values()[0], CourseColor.values()[0]), chosen);
    }

    @Test
    public void mondayIsOnTheRightWhenTheLayoutRunsRightToLeft() {
        var calendar = new MonthCalendarView(context);
        calendar.setSelected(LocalDate.of(2026, 10, 14));
        var chosen = new ArrayList<LocalDate>();
        calendar.setOnDateSelectedListener(chosen::add);
        // The second row of October 2026 starts with Monday the 5th, below the row of the weekday names.
        var y = (28 + 48 * 1.5f) * dp;

        layOut(calendar, 400);
        var grid = calendar.getChildAt(1);
        tap(grid, 10 * dp, y);

        calendar.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(calendar, 400);
        tap(grid, grid.getWidth() - 10 * dp, y);

        assertEquals(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5)), chosen);
    }

    @Test
    public void theArrowKeysFollowTheMirroredCalendar() {
        var calendar = new MonthCalendarView(context);
        calendar.setSelected(LocalDate.of(2026, 10, 14));
        var chosen = new ArrayList<LocalDate>();
        calendar.setOnDateSelectedListener(chosen::add);
        calendar.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(calendar, 400);

        var grid = calendar.getChildAt(1);
        grid.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            grid.requestFocus();
            // The 13th is the day on the right of the 14th now.
            press(grid, KEYCODE_DPAD_RIGHT);
            press(grid, KEYCODE_ENTER);
        });
        assertEquals(List.of(LocalDate.of(2026, 10, 13)), chosen);
    }

    @Test
    public void theArrowKeysFollowTheMirroredColors() {
        var picker = new ColorSwatchPicker(context);
        picker.setSelectedColor(CourseColor.BLUE);
        var chosen = new ArrayList<CourseColor>();
        picker.setOnColorSelectedListener(chosen::add);
        picker.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(picker, 400);

        picker.setFocusableInTouchMode(true);
        getInstrumentation().runOnMainSync(() -> {
            picker.requestFocus();
            // Blue is the last color, so it is on the left, and teal is the one on its right.
            press(picker, KEYCODE_DPAD_RIGHT);
            press(picker, KEYCODE_ENTER);
        });
        assertEquals(List.of(CourseColor.TEAL), chosen);
    }
}