package de.mcmodersd.unipensum.ui.widget;

/**
 * The arithmetic of {@link PaneLayout}, free of Android so it can be tested: when the window is wide
 * enough to show the calendar and a pane side by side, and how wide that pane is.
 */
public final class PaneMetrics {

    private PaneMetrics() {
    }

    /** Whether a window this wide shows the calendar and the pane next to each other. */
    public static boolean isSplit(int windowWidth, int splitMinWidth) {
        return windowWidth >= splitMinWidth;
    }

    /**
     * The width of the pane: a share of the window, kept between a minimum and a maximum so that forms stay
     * comfortable on a small tablet and do not stretch on a large one. Never wider than the window.
     */
    public static int sideWidth(int windowWidth, float share, int minWidth, int maxWidth) {
        int wanted = Math.round(windowWidth * share);
        return Math.min(windowWidth, Math.max(minWidth, Math.min(maxWidth, wanted)));
    }
}
