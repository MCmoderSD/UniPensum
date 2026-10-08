package de.mcmodersd.unipensum.ui;

import androidx.fragment.app.Fragment;

/**
 * Moves between the pages that open from the week calendar. They stack in the side pane: beside the
 * calendar in a wide window, over it in a narrow one. The calendar itself is never replaced.
 */
public interface Navigator {

    /** Opens a page on top of the current one; {@link #pop} comes back. */
    void push(Fragment fragment);

    /** Closes the top page, and the pane with the last one. */
    void pop();

    /** Opens a page as the first of the stack, replacing whatever the pane shows. */
    void open(Fragment fragment);

    /** Like {@link #open}, but closes the pane if this kind of page is already the only one in it. */
    void toggle(Fragment fragment);

    static Navigator of(Fragment fragment) {
        return (Navigator) fragment.requireActivity();
    }
}