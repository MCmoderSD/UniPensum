package de.mcmodersd.unipensum.ui.week;

import static android.view.Gravity.CENTER_VERTICAL;
import static android.widget.LinearLayout.HORIZONTAL;
import static de.mcmodersd.unipensum.R.bool.week_header_compact;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.mcmodersd.unipensum.R;

final class CompactHeader {

    private static final int GAP_DP = 12;
    private static final int PADDING_DP = 4;
    /** The share of the room of the two texts the name of the semester takes at most, the date range gets the rest. */
    private static final float NAME_SHARE = 0.55f;

    private CompactHeader() { }

    /**
     * @return whether the header is compact: on a low screen, and not for large text, where the list of days takes
     * the place of the grid and the texts need the room
     */
    static boolean wanted(Context context) {
        return context.getResources().getBoolean(week_header_compact) && !WeekAgendaView.wanted(context);
    }

    /** @param root the layout of the week, which holds the header */
    static void apply(View root) {
        var title = (TextView) root.findViewById(R.id.title);
        var subtitle = root.findViewById(R.id.subtitle);
        var dp = root.getResources().getDisplayMetrics().density;

        // The name keeps its own width, so the arrow stays beside it, up to a share of the room of the two texts, which
        // is the room of the week itself: a long name is cut off there and the date range, which takes the rest, stays
        // readable, also where the week stands beside an open page.
        var texts = (HeaderTexts) title.getParent();
        texts.setOrientation(HORIZONTAL);
        texts.setGravity(CENTER_VERTICAL);
        texts.setNameShare(NAME_SHARE);
        // One line, cut off at its end, and not broken inside a long word in the little room that is left.
        title.setMaxLines(1);
        var dateParams = (LinearLayout.LayoutParams) subtitle.getLayoutParams();
        dateParams.width = 0;
        dateParams.weight = 1f;
        dateParams.topMargin = 0;
        dateParams.setMarginStart(Math.round(GAP_DP * dp));
        subtitle.setLayoutParams(dateParams);

        var row = (View) texts.getParent();
        row.setPaddingRelative(
                row.getPaddingStart(), Math.round(PADDING_DP * dp), row.getPaddingEnd(), Math.round(PADDING_DP * dp)
        );
    }
}