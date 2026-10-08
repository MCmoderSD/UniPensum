package de.mcmodersd.unipensum.ui.week;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ZoomMetricsTest {

    private static final float PADDING = 10f;

    @Test
    public void clamp_keepsTheZoomBetweenTheLimits() {
        assertEquals(ZoomMetrics.MIN, ZoomMetrics.clamp(0.4f), 0f);
        assertEquals(1.7f, ZoomMetrics.clamp(1.7f), 0f);
        assertEquals(ZoomMetrics.MAX, ZoomMetrics.clamp(9f), 0f);
    }

    @Test
    public void scrollAfterZoom_staysPutWithoutAChange() {
        assertEquals(120f, ZoomMetrics.scrollAfterZoom(120f, 300f, PADDING, 1f), 0.001f);
    }

    @Test
    public void scrollAfterZoom_keepsTheTimeUnderTheFingersInPlace() {
        var scrollY = 100f;
        var focusY = 200f;
        var ratio = 2f;
        // What lies at the focus before: 300 px into the content, 290 px below the first hour line.
        var newScroll = ZoomMetrics.scrollAfterZoom(scrollY, focusY, PADDING, ratio);
        // The same time is now twice as far below the first line, and still at the focus on the screen.
        assertEquals(PADDING + 290f * ratio, newScroll + focusY, 0.001f);
    }

    @Test
    public void scrollAfterZoom_atTheTopStaysAtTheTop() {
        // Pinching right at the first hour line of an unscrolled grid does not move it.
        assertEquals(0f, ZoomMetrics.scrollAfterZoom(0f, PADDING, PADDING, 3f), 0.001f);
    }

    @Test
    public void scrollAfterZoom_undoesItselfWhenTheZoomIsReversed() {
        var zoomedIn = ZoomMetrics.scrollAfterZoom(80f, 250f, PADDING, 2.5f);
        assertEquals(80f, ZoomMetrics.scrollAfterZoom(zoomedIn, 250f, PADDING, 1f / 2.5f), 0.001f);
    }
}
