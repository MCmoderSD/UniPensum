package de.mcmodersd.unipensum.ui.widget;

import static android.text.InputType.*;
import static android.text.Layout.*;
import static android.view.KeyEvent.*;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.CourseColor;

/** The controls give a finger the 48dp Android asks for, and tell a screen reader what they are. */
@RunWith(AndroidJUnit4.class)
public class ControlsTest {

    private final Context context = new ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.Theme_UniPensum
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
            assertEquals(
                    i == 1 ? AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE : AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE,
                    node.getChecked()
            );
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

    /** The message that is set again comes with a later frame, and a little later still after another one. */
    private static void awaitText(TextView view, String expected) throws InterruptedException {
        var arrived = new CountDownLatch(1);
        getInstrumentation().runOnMainSync(() -> {
            if (expected.contentEquals(view.getText())) {
                arrived.countDown();
                return;
            }
            view.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence text, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence text, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable text) {
                    if (expected.contentEquals(text)) arrived.countDown();
                }
            });
        });
        assertTrue("The text \"" + expected + "\" did not come", arrived.await(2, TimeUnit.SECONDS));
        assertEquals(expected, view.getText().toString());
    }

    @Test
    public void aMessageThatIsShownAgainIsClearedAndSetAgainLater() throws InterruptedException {
        var view = new TextView(context);
        var message = new LiveMessage(view);

        getInstrumentation().runOnMainSync(() -> message.show("Enter a name."));
        assertEquals(View.VISIBLE, view.getVisibility());
        assertEquals("Enter a name.", view.getText().toString());

        // A screen reader does not say the same text twice in a row, so it has to change in between.
        getInstrumentation().runOnMainSync(() -> {
            message.show("Enter a name.");
            assertEquals("", view.getText().toString());
        });
        awaitText(view, "Enter a name.");
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    public void twoMessagesThatAreShownAgainAreSetInTurn() throws InterruptedException {
        var first = new TextView(context);
        var second = new TextView(context);
        var firstMessage = new LiveMessage(first);
        var secondMessage = new LiveMessage(second);
        getInstrumentation().runOnMainSync(() -> {
            firstMessage.show("Enter a name.");
            secondMessage.show("Add at least one event.");
        });

        // Both are cleared and set again, but not in the same frame: a screen reader says one change of a frame.
        var times = new long[2];
        getInstrumentation().runOnMainSync(() -> {
            firstMessage.show("Enter a name.");
            secondMessage.show("Add at least one event.");
        });
        var start = System.nanoTime();
        awaitText(first, "Enter a name.");
        times[0] = System.nanoTime() - start;
        awaitText(second, "Add at least one event.");
        times[1] = System.nanoTime() - start;
        assertTrue(times[1] > times[0]);
    }

    @Test
    public void aDifferentMessageIsShownAtOnceAndAClearedOneStaysAway() throws InterruptedException {
        var view = new TextView(context);
        var message = new LiveMessage(view);

        getInstrumentation().runOnMainSync(() -> {
            message.show("Enter a name.");
            message.show("Enter a name.");
            // the older one must not come back after this one
            message.show("Add at least one event.");
            assertEquals("Add at least one event.", view.getText().toString());
        });
        Thread.sleep(500);
        assertEquals("Add at least one event.", view.getText().toString());

        getInstrumentation().runOnMainSync(() -> {
            message.show("Add at least one event.");
            message.show(null);
        });
        Thread.sleep(500);
        assertEquals(View.GONE, view.getVisibility());
        assertEquals("", view.getText().toString());
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

    private Context withFontScale() {
        var configuration = new Configuration(context.getResources().getConfiguration());
        configuration.fontScale = (float) 2.0;
        return new ContextThemeWrapper(context.createConfigurationContext(configuration), R.style.Theme_UniPensum);
    }

    /**
     * A row and a segmented control change their own structure while they measure, so a second measure of the same
     * width must find the structure of the first and not change it back and forth.
     */
    @Test
    public void measuringARowAndASegmentedControlAgainGivesTheSameLayoutAtLargeText() {
        var large = withFontScale();
        var row = new UpRow(large);
        row.setTitle("UniPensum");
        row.setValue("Version 1.0-SNAPSHOT");
        var texts = (ViewGroup) row.getChildAt(0);
        var control = new UpSegmentedControl(large);
        control.setOptions("Lecture", "Exercise", "Laboratory", "Tutorial");

        for (var mode : new int[]{EXACTLY, AT_MOST}) {
            for (var width : new int[]{150, 300, 1000, 300, 150}) {
                var widthSpec = makeMeasureSpec(Math.round(width * dp), mode);
                var heightSpec = makeMeasureSpec(4000, AT_MOST);

                row.measure(widthSpec, heightSpec);
                var rowHeight = row.getMeasuredHeight();
                var rowTexts = texts.getChildCount();
                row.forceLayout();
                row.measure(widthSpec, heightSpec);
                assertEquals(rowHeight, row.getMeasuredHeight());
                assertEquals(rowTexts, texts.getChildCount());

                control.measure(widthSpec, heightSpec);
                var orientation = control.getOrientation();
                var controlWidth = control.getMeasuredWidth();
                var controlHeight = control.getMeasuredHeight();
                control.forceLayout();
                control.measure(widthSpec, heightSpec);
                assertEquals(orientation, control.getOrientation());
                assertEquals(controlWidth, control.getMeasuredWidth());
                assertEquals(controlHeight, control.getMeasuredHeight());
            }
        }
    }

    /** A scroll view or a wrap_content parent measures without a width first, and the width decides only then. */
    @Test
    public void aRowMeasuredWithoutAWidthFirstEndsAsIfMeasuredWithTheWidthAlone() {
        var large = withFontScale();
        var heightSpec = makeMeasureSpec(4000, AT_MOST);
        for (var width : new int[]{150, 1000}) {
            var widthSpec = makeMeasureSpec(Math.round(width * dp), EXACTLY);

            var alone = new UpRow(large);
            alone.setTitle("UniPensum");
            alone.setValue("Version 1.0-SNAPSHOT");
            alone.measure(widthSpec, heightSpec);

            var afterwards = new UpRow(large);
            afterwards.setTitle("UniPensum");
            afterwards.setValue("Version 1.0-SNAPSHOT");
            afterwards.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
            afterwards.measure(widthSpec, heightSpec);

            assertEquals(alone.getMeasuredHeight(), afterwards.getMeasuredHeight());
            assertEquals(
                    ((ViewGroup) alone.getChildAt(0)).getChildCount(),
                    ((ViewGroup) afterwards.getChildAt(0)).getChildCount()
            );
        }
    }

    @Test
    public void aValueThatChangesAfterTheFirstLayoutFlowsTheRowAgain() {
        var large = withFontScale();
        var row = new UpRow(large);
        row.setTitle("UniPensum");
        row.setValue("1.0");
        var texts = (ViewGroup) row.getChildAt(0);
        var widthSpec = makeMeasureSpec(Math.round(400 * dp), EXACTLY);
        var heightSpec = makeMeasureSpec(4000, AT_MOST);

        row.measure(widthSpec, heightSpec);
        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
        // the title and the subtitle; the value is beside them
        assertEquals(2, texts.getChildCount());

        row.setValue("Version 1.0-SNAPSHOT of the app");
        assertTrue(row.isLayoutRequested());
        row.measure(widthSpec, heightSpec);
        assertEquals(3, texts.getChildCount());

        row.setValue("");
        assertTrue(row.isLayoutRequested());
        row.measure(widthSpec, heightSpec);
        assertEquals(2, texts.getChildCount());
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

    private static TextView titleOf(UpRow row) {
        return (TextView) ((ViewGroup) row.getChildAt(0)).getChildAt(0);
    }

    @Test
    public void aPhoneNumberReadsFromLeftToRightInEveryLayout() {
        var number = new UpRow(context);
        number.setTitle("+49 30 5550123");
        number.keepTitleLeftToRight();
        var plain = new UpRow(context);
        plain.setTitle("+49 30 5550123");
        for (var row : new UpRow[]{number, plain}) {
            row.setLayoutDirection(LAYOUT_DIRECTION_RTL);
            layOut(row, 400);
        }
        assertEquals(DIR_LEFT_TO_RIGHT, titleOf(number).getLayout().getParagraphDirection(0));
        // Without the call the digits take the direction of the layout, which is the reversed number of the audit.
        assertEquals(DIR_RIGHT_TO_LEFT, titleOf(plain).getLayout().getParagraphDirection(0));
    }

    @Test
    public void aFieldForAnAddressOrANumberReadsFromLeftToRight() {
        var types = new int[]{
                TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_URI,
                TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                TYPE_CLASS_PHONE
        };
        for (var type : types) {
            var field = new UpTextField(context);
            field.setInputType(type);
            field.setLayoutDirection(LAYOUT_DIRECTION_RTL);
            layOut(field, 400);
            assertEquals(View.TEXT_DIRECTION_LTR, field.getChildAt(1).getTextDirection());
        }

        var name = new UpTextField(context);
        name.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(name, 400);
        assertNotEquals(View.TEXT_DIRECTION_LTR, name.getChildAt(1).getTextDirection());
    }
}