package de.mcmodersd.unipensum.ui.week;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.domain.logic.Weeks;
import de.mcmodersd.unipensum.domain.model.NameStyle;

// A data or settings update changes every week at once, so there is no narrower change to report.
@SuppressLint("NotifyDataSetChanged")
final class WeekPagerAdapter extends RecyclerView.Adapter<WeekPagerAdapter.PageHolder> {

    static final class PageHolder extends RecyclerView.ViewHolder {
        final WeekPageView page;

        PageHolder(WeekPageView page) {
            super(page);
            this.page = page;
        }
    }

    private final List<WeekPageView> attached = new ArrayList<>();
    private Timetable timetable;
    private TimeWindow window = TimeWindow.DEFAULT;
    private NameStyle nameStyle = NameStyle.LAST_NAME;
    private WeekGridView.OnSessionClickListener clickListener;
    private int scrollY;
    private final IntSupplier currentScrollY = () -> scrollY;

    void setTimetable(Timetable timetable) {
        this.timetable = timetable;
        notifyDataSetChanged();
    }

    void setWindow(TimeWindow window) {
        this.window = window;
        notifyDataSetChanged();
    }

    void setNameStyle(NameStyle nameStyle) {
        this.nameStyle = nameStyle;
        notifyDataSetChanged();
    }

    void setOnSessionClickListener(WeekGridView.OnSessionClickListener listener) {
        this.clickListener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WeekPageView page = new WeekPageView(parent.getContext(), new GridMetrics(parent.getContext()));
        page.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        page.setScrollListener(this::onPageScrolled);
        return new PageHolder(page);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.page.bind(Weeks.mondayOf(position), LocalDate.now(), window, nameStyle, timetable, clickListener);
        holder.page.followWhenReady(currentScrollY);
    }

    @Override
    public int getItemCount() {
        return Weeks.PAGE_COUNT;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PageHolder holder) {
        attached.add(holder.page);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PageHolder holder) {
        attached.remove(holder.page);
    }

    /** Keeps the vertical position the same on every page, so swiping weeks never jumps. */
    private void onPageScrolled(WeekPageView source, int y) {
        scrollY = y;
        for (WeekPageView page : attached) {
            if (page != source && page.scrollY() != y) page.followWhenReady(currentScrollY);
        }
    }
}
