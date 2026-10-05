package de.mcmodersd.unipensum.ui.week;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.AppSettings.TimeWindow;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.domain.model.NameStyle;

public class WeekViewModel extends AndroidViewModel {

    private final LiveData<Timetable> timetable;
    private final LiveData<TimeWindow> timeWindow;
    private final LiveData<NameStyle> lecturerNameStyle;
    private int page = -1;

    public WeekViewModel(@NonNull Application application) {
        super(application);
        UniPensumApp app = UniPensumApp.from(application);
        timetable = app.repository().timetable();
        timeWindow = app.settings().timeWindow();
        lecturerNameStyle = app.settings().lecturerNameStyle();
    }

    public LiveData<NameStyle> lecturerNameStyle() {
        return lecturerNameStyle;
    }

    /** Emits once the first load has finished; never emits {@code null}. */
    public LiveData<Timetable> timetable() {
        return timetable;
    }

    public LiveData<TimeWindow> timeWindow() {
        return timeWindow;
    }

    /**
     * The week the user is looking at, or -1 before the first one was shown. Kept here because the view
     * is destroyed while an editor covers the screen and must come back on the same week.
     */
    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
