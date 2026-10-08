package de.mcmodersd.unipensum.ui.week;

/** The numbers behind zooming the week grid vertically. Plain Java, so they can be tested. */
final class ZoomMetrics {

    /** The normal grid. There is no zooming out below it. */
    static final float MIN = 1f;
    static final float MAX = 4f;

    private ZoomMetrics() { }

    static float clamp(float zoom) {
        return Math.max(MIN, Math.min(MAX, zoom));
    }

    /**
     * The scroll position that keeps the time under {@code focusY} where it is while the hours grow by
     * {@code ratio} (new zoom divided by old zoom).
     *
     * @param scrollY the current scroll position
     * @param focusY  the distance of the pinch center from the top of the scrolling area
     * @param padding the space above the first hour line, which does not grow
     */
    static float scrollAfterZoom(float scrollY, float focusY, float padding, float ratio) {
        var contentY = scrollY + focusY;
        return padding + (contentY - padding) * ratio - focusY;
    }
}
