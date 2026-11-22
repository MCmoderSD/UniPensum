package de.mcmodersd.unipensum.ui.week;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static de.mcmodersd.unipensum.domain.model.CourseColor.RED;
import static de.mcmodersd.unipensum.domain.model.Mode.IN_PERSON;
import static de.mcmodersd.unipensum.domain.model.SessionDetails.*;
import static de.mcmodersd.unipensum.domain.model.SessionType.LECTURE;
import static de.mcmodersd.unipensum.ui.week.GridMetrics.HOUR_LABEL_SP;
import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.LocaleList;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.model.Session;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.ui.format.CourseColors;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/** The week follows the font size of the system, and a screen reader hears on which day a session is. */
@RunWith(AndroidJUnit4.class)
public class WeekViewsTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    private final Context normal = new ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.Theme_UniPensum
    );

    private Context withFontScale(float scale) {
        var configuration = new Configuration(normal.getResources().getConfiguration());
        configuration.fontScale = scale;
        return new ContextThemeWrapper(normal.createConfigurationContext(configuration), R.style.Theme_UniPensum);
    }

    private static SessionView session(long id, LocalDate day, String course, String room) {
        var details = new SessionDetails(
                LECTURE, 8 * 60, 11 * 60 + 15, IN_PERSON, false, room, null, NO_LECTURER, null, NO_REMINDER
        );
        return new SessionView(new Session(id, 1, day, details), 1, 1, course, RED, null);
    }

    /** Five days; the second one is empty. */
    private static ArrayList<List<SessionView>> week() {
        var perDay = new ArrayList<List<SessionView>>();
        perDay.add(List.of(session(1, MONDAY, "Algorithms", "H1"), session(2, MONDAY, "Databases", "H2")));
        perDay.add(List.of());
        perDay.add(List.of(session(3, MONDAY.plusDays(2), "Physics", "A2")));
        perDay.add(List.of());
        perDay.add(List.of());
        return perDay;
    }

    private static void measure(View view, int width) {
        view.measure(makeMeasureSpec(width, AT_MOST), makeMeasureSpec(10_000, AT_MOST));
    }

    @Test
    public void largeTextWidensTheGutterAndTheHeader() {
        var small = new GridMetrics(withFontScale(1f));
        var large = new GridMetrics(withFontScale(1.3f));
        assertTrue(large.gutter >= small.gutter);
        assertTrue(large.headerHeight > small.headerHeight);
    }

    @Test
    public void gutterFitsTheWidestHourLabel() {
        var context = withFontScale(1.3f);
        var metrics = new GridMetrics(context);
        var paint = new Paint();
        paint.setTextSize(
                TypedValue.applyDimension(COMPLEX_UNIT_SP, HOUR_LABEL_SP, context.getResources().getDisplayMetrics())
        );
        var widest = 0f;
        for (var hour = 0; hour < 24; hour++) {
            widest = Math.max(widest, paint.measureText(TimeFormat.hour(context, hour)));
        }
        assertTrue(metrics.gutter >= widest);
    }

    @Test
    public void listOfDaysFromFontScale1Point3() {
        assertFalse(WeekAgendaView.wanted(withFontScale(1.15f)));
        assertTrue(WeekAgendaView.wanted(withFontScale(1.3f)));
        assertTrue(WeekAgendaView.wanted(withFontScale(2f)));
    }

    @Test
    public void listHasAHeadingForEveryDayAndACardForEverySession() {
        var context = withFontScale(1.5f);
        var agenda = new WeekAgendaView(context);
        agenda.bind(MONDAY, MONDAY.plusDays(2), NameStyle.LAST_NAME, week(), null);
        measure(agenda, 1000);

        var headings = new ArrayList<TextView>();
        var cards = 0;
        var emptyDays = 0;
        for (var i = 0; i < agenda.getChildCount(); i++) {
            var child = agenda.getChildAt(i);
            if (child instanceof AgendaCardView) {
                cards++;
            } else if (ViewCompat.isAccessibilityHeading(child)) {
                headings.add((TextView) child);
            } else {
                emptyDays++;
            }
        }
        assertEquals(5, headings.size());
        assertEquals(3, cards);
        assertEquals(3, emptyDays);
        // today is the Wednesday
        assertTrue(headings.get(2).getText().toString().contains(context.getString(R.string.agenda_today, "").trim()));
        assertFalse(headings.get(0).getText().toString().contains(context.getString(R.string.agenda_today, "").trim()));
    }

    @Test
    public void everyLineOfACardIsShownInFull() {
        var context = withFontScale(2f);
        var card = new AgendaCardView(context, session(1, MONDAY, "Software Engineering", "Library 2"), NameStyle.LAST_NAME);
        measure(card, 600);
        var texts = new ArrayList<String>();
        for (var i = 0; i < card.getChildCount(); i++) {
            var line = (TextView) card.getChildAt(i);
            texts.add(line.getText().toString());
            assertEquals("a line must not be cut off", Integer.MAX_VALUE, line.getMaxLines());
        }
        assertTrue(texts.contains("Software Engineering"));
        assertTrue(texts.contains("Library 2"));
    }

    @Test
    public void sessionDescriptionSaysTheDay() {
        var description = SessionBlockView.describe(normal, session(1, MONDAY, "Algorithms", "H1"));
        // the weekday of 2026-10-12 is a Monday, in the language of the device
        var day = DayOfWeek.MONDAY.getDisplayName(
                TextStyle.FULL, normal.getResources().getConfiguration().getLocales().get(0)
        );
        assertTrue(description, description.contains(day));
        assertTrue(description, description.contains("Algorithms"));
    }

    // --- the language of a name ---

    private Context withLocale(String languageTag) {
        var configuration = new Configuration(normal.getResources().getConfiguration());
        configuration.setLocales(new LocaleList(Locale.forLanguageTag(languageTag)));
        return new ContextThemeWrapper(normal.createConfigurationContext(configuration), R.style.Theme_UniPensum);
    }

    private static TextView titleOf(SessionBlockView block) {
        return (TextView) block.getChildAt(0);
    }

    @Test
    public void aNameInLatinLettersIsHyphenatedInEnglishOnADeviceInAnotherScript() {
        for (var tag : new String[]{"ar", "he", "fa"}) {
            var context = withLocale(tag);
            var block = new SessionBlockView(
                    context, new GridMetrics(context), session(1, MONDAY, "Software Engineering", "H1"),
                    NameStyle.LAST_NAME, false
            );
            assertEquals(tag, "en", titleOf(block).getTextLocale().getLanguage());
        }
    }

    @Test
    public void theTitleOfABlockKeepsTheLanguageOfTheDeviceWhereItHasPatterns() {
        var german = withLocale("de");
        var block = new SessionBlockView(
                german, new GridMetrics(german), session(1, MONDAY, "Softwaretechnik", "H1"), NameStyle.LAST_NAME, false
        );
        assertEquals(new TextView(german).getTextLocale(), titleOf(block).getTextLocale());

        var arabic = withLocale("ar");
        var arabicName = new SessionBlockView(
                arabic, new GridMetrics(arabic), session(1, MONDAY, "هندسة البرمجيات", "H1"), NameStyle.LAST_NAME, false
        );
        assertEquals(new TextView(arabic).getTextLocale(), titleOf(arabicName).getTextLocale());
    }

    // --- a phone held sideways ---

    private Context withScreen(int widthDp, int heightDp, float fontScale) {
        var configuration = new Configuration(normal.getResources().getConfiguration());
        configuration.screenWidthDp = widthDp;
        configuration.screenHeightDp = heightDp;
        configuration.fontScale = fontScale;
        return new ContextThemeWrapper(normal.createConfigurationContext(configuration), R.style.Theme_UniPensum);
    }

    @Test
    public void theHeaderIsCompactOnAScreenLessThan480dpHighButNotForLargeText() {
        assertTrue(CompactHeader.wanted(withScreen(640, 360, 1f)));
        assertTrue(CompactHeader.wanted(withScreen(640, 479, 1f)));
        assertFalse(CompactHeader.wanted(withScreen(640, 480, 1f)));
        assertFalse(CompactHeader.wanted(withScreen(400, 800, 1f)));
        // From font scale 1.3 the days are a list, and the texts need the room.
        assertFalse(CompactHeader.wanted(withScreen(640, 360, 1.3f)));
    }

    @Test
    public void theDateRangeStandsBesideTheNameAndTheHeaderLosesHeight() {
        var context = withScreen(640, 360, 1f);
        var dp = context.getResources().getDisplayMetrics().density;
        var width = Math.round(640 * dp);
        var height = Math.round(360 * dp);
        var root = LayoutInflater.from(context).inflate(R.layout.fragment_week, null);
        var title = (TextView) root.findViewById(R.id.title);
        var subtitle = (TextView) root.findViewById(R.id.subtitle);
        title.setText("Winter 26/27");
        subtitle.setText("Nov 23 – Nov 27 · Week 48");
        var row = (View) ((View) title.getParent()).getParent();

        layOut(root, width, height);
        var high = row.getHeight();
        assertTrue(subtitle.getTop() >= title.getBottom());

        CompactHeader.apply(root);
        layOut(root, width, height);
        assertTrue(row.getHeight() < high);
        // as high as the buttons of 48dp and the little space around them
        assertTrue(row.getHeight() <= Math.round(48 * dp) + Math.round(2 * 4 * dp) + 1);
        assertEquals(LinearLayout.HORIZONTAL, ((LinearLayout) title.getParent()).getOrientation());
        assertTrue(subtitle.getLeft() >= title.getRight());

        // A long name is cut off and the date range stays whole.
        title.setText("Introduction to the Mathematics of Computer Science and Its Applications");
        layOut(root, width, height);
        assertTrue(title.getWidth() <= Math.round(0.55f * (640 - 3 * 48 - 24) * dp) + 1);
        assertTrue(subtitle.getLeft() >= title.getRight());
        assertEquals(1, subtitle.getLineCount());
    }

    /** A phone held sideways is 914dp wide, but the week beside an open page has only about 550dp of it. */
    @Test
    public void theDateRangeKeepsRoomBesideALongNameWhenThePageIsOpen() {
        var context = withScreen(914, 411, 1f);
        var dp = context.getResources().getDisplayMetrics().density;
        var width = Math.round(550 * dp);
        var height = Math.round(411 * dp);
        var root = LayoutInflater.from(context).inflate(R.layout.fragment_week, null);
        var title = (TextView) root.findViewById(R.id.title);
        var subtitle = (TextView) root.findViewById(R.id.subtitle);
        title.setText("Wintersemester 2026/27 Informatik Master");
        subtitle.setText("Nov 23 – Nov 27 · Week 48");

        CompactHeader.apply(root);
        layOut(root, width, height);

        // the room of the two texts is 550dp less the buttons and the padding, and the name takes 55 % of it
        var room = Math.round((550 - 3 * 48 - 24) * dp);
        assertTrue(title.getWidth() <= Math.round(0.55f * room) + 1);
        assertTrue(subtitle.getWidth() >= Math.round(100 * dp));
        assertTrue(subtitle.getLeft() >= title.getRight());
        assertTrue(subtitle.getRight() <= ((View) title.getParent()).getWidth());
        // The name is cut off at its end and the header stays as high as its buttons.
        assertEquals(1, title.getLineCount());
        var row = (View) ((View) title.getParent()).getParent();
        assertTrue(row.getHeight() <= Math.round(48 * dp) + Math.round(2 * 4 * dp) + 1);
    }

    // --- right to left ---

    private static void layOut(View view, int width, int height) {
        view.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(height, EXACTLY));
        view.layout(0, 0, width, height);
    }

    private static Bitmap draw(View view) {
        var bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    @Test
    public void theWeekRunsFromTheRightWhenTheLayoutRunsFromRightToLeft() {
        var width = 1000;
        var grid = new WeekGridView(normal, new GridMetrics(normal));
        grid.bind(MONDAY, TimeWindow.DEFAULT, NameStyle.LAST_NAME, week(), null);

        layOut(grid, width, 3000);
        // The first two blocks share Monday, the third is on Wednesday.
        var first = grid.getChildAt(0);
        var second = grid.getChildAt(1);
        var wednesday = grid.getChildAt(2);
        assertTrue(first.getLeft() < second.getLeft());
        assertTrue(second.getRight() <= wednesday.getLeft());

        grid.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(grid, width, 3000);
        assertTrue(first.getLeft() > second.getLeft());
        assertTrue(wednesday.getRight() <= second.getLeft());
        // Monday is next to the hours, which are now on the right.
        assertTrue(first.getRight() > width - new GridMetrics(normal).gutter - 2 * first.getWidth());
    }

    @Test
    public void theBarOfASessionRunsAlongTheStartEdge() {
        var dp = normal.getResources().getDisplayMetrics().density;
        var width = Math.round(100 * dp);
        var height = Math.round(100 * dp);
        var block = new SessionBlockView(
                normal, new GridMetrics(normal), session(1, MONDAY, "Algorithms", "H1"), NameStyle.LAST_NAME, false
        );
        block.fitTo(width, height);
        var bar = CourseColors.resolve(normal, RED);
        var x = Math.round(3.3f * dp);
        var y = height / 2;

        layOut(block, width, height);
        var leftToRight = draw(block);
        assertEquals(bar, leftToRight.getPixel(x, y));
        assertNotEquals(bar, leftToRight.getPixel(width - 1 - x, y));
        var title = block.getChildAt(0);
        assertTrue(title.getLeft() > x);

        block.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(block, width, height);
        var rightToLeft = draw(block);
        assertEquals(bar, rightToLeft.getPixel(width - 1 - x, y));
        assertNotEquals(bar, rightToLeft.getPixel(x, y));
        assertTrue(title.getRight() < width - 1 - x);
    }

    @Test
    public void theBarOfACardRunsAlongTheStartEdge() {
        var dp = normal.getResources().getDisplayMetrics().density;
        var width = Math.round(300 * dp);
        var card = new AgendaCardView(normal, session(1, MONDAY, "Algorithms", "H1"), NameStyle.LAST_NAME);
        var bar = CourseColors.resolve(normal, RED);
        var x = Math.round(6.2f * dp);

        card.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        card.layout(0, 0, width, card.getMeasuredHeight());
        assertEquals(bar, draw(card).getPixel(x, card.getHeight() / 2));

        card.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        card.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        card.layout(0, 0, width, card.getMeasuredHeight());
        assertEquals(bar, draw(card).getPixel(width - 1 - x, card.getHeight() / 2));
    }

    @Test
    public void theFirstDayOfTheHeaderIsOnTheRightInALayoutFromRightToLeft() {
        var dp = normal.getResources().getDisplayMetrics().density;
        var metrics = new GridMetrics(normal);
        var width = 1000;
        var header = new WeekHeaderView(normal, metrics);
        // Today is the Monday, which the header marks with a filled circle.
        header.bind(MONDAY, MONDAY);
        var accent = ContextCompat.getColor(normal, R.color.accent);
        var mondayX = Math.round(metrics.gutter + metrics.columnWidth(width) / 2);
        // Inside the circle and beside the number.
        var offset = Math.round(12 * dp);
        var y = Math.round(metrics.headerHeight * 0.70f);

        layOut(header, width, metrics.headerHeight);
        assertEquals(accent, draw(header).getPixel(mondayX - offset, y));

        header.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        layOut(header, width, metrics.headerHeight);
        var mirrored = draw(header);
        assertEquals(accent, mirrored.getPixel(width - mondayX - offset, y));
        assertNotEquals(accent, mirrored.getPixel(mondayX - offset, y));
    }
}