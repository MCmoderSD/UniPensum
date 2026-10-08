package de.mcmodersd.unipensum.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaneMetricsTest {

    private static final int SPLIT_MIN = 720;
    private static final float SHARE = 0.4f;
    private static final int SIDE_MIN = 360;
    private static final int SIDE_MAX = 480;

    private static int side(int width) {
        return PaneMetrics.sideWidth(width, SHARE, SIDE_MIN, SIDE_MAX);
    }

    @Test
    public void isSplit_startsAtTheMinimumWidth() {
        assertFalse(PaneMetrics.isSplit(719, SPLIT_MIN));
        assertTrue(PaneMetrics.isSplit(720, SPLIT_MIN));
        assertTrue(PaneMetrics.isSplit(1280, SPLIT_MIN));
    }

    @Test
    public void sideWidth_isTheShareInTheMiddleOfTheRange() {
        assertEquals(400, side(1000));
        assertEquals(360, side(900));
    }

    @Test
    public void sideWidth_doesNotGetNarrowerThanTheMinimum() {
        assertEquals(360, side(720));
        assertEquals(360, side(800));
    }

    @Test
    public void sideWidth_doesNotGetWiderThanTheMaximum() {
        assertEquals(480, side(1280));
        assertEquals(480, side(2000));
    }

    @Test
    public void sideWidth_neverExceedsTheWindow() {
        assertEquals(300, side(300));
    }

    @Test
    public void everySplitWindow_leavesAtLeastTheMinimumForTheCalendar() {
        for (var width = SPLIT_MIN; width <= 3000; width++) {
            assertTrue("width " + width, width - side(width) >= SIDE_MIN);
        }
    }

    @Test
    public void sideWidth_growsWithTheWindow() {
        var previous = 0;
        for (var width = SPLIT_MIN; width <= 3000; width++) {
            var current = side(width);
            assertTrue("width " + width, current >= previous);
            previous = current;
        }
    }
}
