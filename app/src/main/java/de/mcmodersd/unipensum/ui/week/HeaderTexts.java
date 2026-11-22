package de.mcmodersd.unipensum.ui.week;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.mcmodersd.unipensum.R;

/**
 * The name of the semester and the date range at the top of the week. In the compact header they stand side by side,
 * and the name takes at most a share of the room this view has. That is the room of the week itself: beside an open
 * page the week is narrower than the screen, and a name that took its share of the screen would leave nothing for the
 * date range.
 */
public final class HeaderTexts extends LinearLayout {

    private float nameShare;

    public HeaderTexts(Context context) {
        this(context, null);
    }

    public HeaderTexts(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** @param share the part of the width the name takes at most, from 0 to 1, or 0 for no limit */
    void setNameShare(float share) {
        nameShare = share;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (nameShare > 0 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            var name = (TextView) findViewById(R.id.title);
            var limit = Math.round(nameShare * MeasureSpec.getSize(widthMeasureSpec));
            if (name.getMaxWidth() != limit) name.setMaxWidth(limit);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}