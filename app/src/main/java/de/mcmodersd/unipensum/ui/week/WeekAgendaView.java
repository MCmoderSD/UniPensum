package de.mcmodersd.unipensum.ui.week;

import static android.view.ViewGroup.LayoutParams.*;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.R.style.*;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.data.SessionView;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.ui.format.TimeFormat;

/**
 * The week as a list of days: the day with its date as a heading, below it one card for every session, or a line
 * saying there is none. It takes the place of the grid when the text is large, because five narrow columns cannot
 * hold it: a column would be left with a few letters of the course name. Here every session has the whole width.
 */
final class WeekAgendaView extends LinearLayout {

    /** From this font size of the system on, the week is shown as a list. 1.3 is "Largest" on most phones. */
    static final float MIN_FONT_SCALE = 1.3f;

    private final float dp = getResources().getDisplayMetrics().density;

    static boolean wanted(Context context) {
        return context.getResources().getConfiguration().fontScale >= MIN_FONT_SCALE;
    }

    WeekAgendaView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Math.round(16 * dp), Math.round(8 * dp), Math.round(16 * dp), Math.round(24 * dp));
    }

    /**
     * @param monday the Monday of the week shown
     * @param perDay exactly five lists, Monday to Friday, each ordered by start time
     */
    void bind(LocalDate monday, LocalDate today, NameStyle nameStyle, ArrayList<List<SessionView>> perDay,
              WeekGridView.OnSessionClickListener listener) {
        removeAllViews();
        for (var day = 0; day < perDay.size(); day++) {
            var date = monday.plusDays(day);
            addView(heading(date, date.equals(today)), params(day == 0 ? 0 : 24));

            var sessions = perDay.get(day);
            if (sessions.isEmpty()) {
                var none = new TextView(getContext());
                none.setTextAppearance(TextAppearance_Up_Caption);
                none.setText(agenda_no_events);
                addView(none, params(8));
            }
            for (var session : sessions) {
                var card = new AgendaCardView(getContext(), session, nameStyle);
                if (listener != null) card.setOnClickListener(v -> listener.onSessionClick(session));
                addView(card, params(8));
            }
        }
    }

    private TextView heading(LocalDate date, boolean isToday) {
        var text = TimeFormat.dateWithWeekday(getContext(), date);
        var view = new TextView(getContext());
        view.setTextAppearance(TextAppearance_Up_Title);
        view.setText(isToday ? getContext().getString(agenda_today, text) : text);
        ViewCompat.setAccessibilityHeading(view, true);
        return view;
    }

    private LayoutParams params(int topMarginDp) {
        var params = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = Math.round(topMarginDp * dp);
        return params;
    }
}