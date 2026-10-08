package de.mcmodersd.unipensum.ui.week;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.AppSettings;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.domain.logic.Weeks;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.reminder.NotificationAccess;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.course.CourseDraftViewModel;
import de.mcmodersd.unipensum.ui.course.CourseEditorFragment;
import de.mcmodersd.unipensum.ui.course.CoursesFragment;
import de.mcmodersd.unipensum.ui.format.SemesterNames;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.semester.SemesterEditorSheet;
import de.mcmodersd.unipensum.ui.semester.SemesterSheet;
import de.mcmodersd.unipensum.ui.session.SessionDetailSheet;
import de.mcmodersd.unipensum.ui.settings.SettingsFragment;
import de.mcmodersd.unipensum.ui.widget.ConfirmSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;

/**
 * The app's one main screen: the semester name, the week range and the swipeable week grid. It stays
 * alive while pages open next to or over it, so it follows every change made there at once.
 */
public class WeekFragment extends Fragment {

    /** How long the zoom label stays once the zoom is back at 100 %. */
    private static final long ZOOM_INDICATOR_MS = 1200;

    private static final String KEY_NOTIFICATIONS = "week_notifications";

    private final Runnable hideZoom = this::fadeOutZoom;
    private final ActivityResultLauncher<String> askNotifications =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });
    private WeekViewModel viewModel;
    private ViewPager2 pager;
    private TextView zoomIndicator;
    private TextView title;
    private TextView subtitle;
    private View empty;
    private View addCourse;
    private View openCourses;
    private Timetable timetable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        title = view.findViewById(R.id.title);
        subtitle = view.findViewById(R.id.subtitle);
        empty = view.findViewById(R.id.empty);
        addCourse = view.findViewById(R.id.add_course);
        openCourses = view.findViewById(R.id.open_courses);
        pager = view.findViewById(R.id.pager);
        zoomIndicator = view.findViewById(R.id.zoom_indicator);

        viewModel = new ViewModelProvider(this).get(WeekViewModel.class);
        var adapter = new WeekPagerAdapter();
        adapter.setZoom(viewModel.zoom());
        adapter.setOnZoomChangedListener(zoom -> {
            viewModel.setZoom(zoom);
            showZoom(zoom);
        });
        zoomIndicator.setOnClickListener(v -> {
            Haptics.tap(v);
            resetZoom();
        });
        // The view is rebuilt while the grid is zoomed (turning, theme, language): the label has to come back.
        if (viewModel.zoom() > ZoomMetrics.MIN) showZoom(viewModel.zoom());
        adapter.setOnSessionClickListener(session ->
                SessionDetailSheet.show(getParentFragmentManager(), session.session().id())
        );
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        // The view is rebuilt when the screen is recreated (turning, theme, language); come back to the same week.
        pager.setCurrentItem(viewModel.page() >= 0 ? viewModel.page() : Weeks.initialPosition(LocalDate.now()), false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewModel.setPage(position);
                updateHeader(position);
            }
        });

        getParentFragmentManager().setFragmentResultListener(
                KEY_NOTIFICATIONS, getViewLifecycleOwner(),
                (key, result) -> allowNotifications()
        );
        title.setOnClickListener(v -> {
            Haptics.tap(v);
            SemesterSheet.show(getParentFragmentManager());
        });
        addCourse.setOnClickListener(v -> openNewCourse());
        openCourses.setOnClickListener(v -> openCourses());
        view.findViewById(R.id.open_settings).setOnClickListener(v ->
                Navigator.of(this).toggle(new SettingsFragment())
        );
        view.findViewById(R.id.create_semester).setOnClickListener(v ->
                SemesterEditorSheet.show(getParentFragmentManager(), null, timetable.semesters())
        );
        getParentFragmentManager().setFragmentResultListener(
                SemesterSheet.REQUEST_JUMP, getViewLifecycleOwner(),
                (key, result) -> pager.setCurrentItem(
                        Weeks.positionOf(LocalDate.ofEpochDay(result.getLong(SemesterSheet.RESULT_EPOCH_DAY))), false
                )
        );

        viewModel.timeWindow().observe(getViewLifecycleOwner(), adapter::setWindow);
        viewModel.lecturerNameStyle().observe(getViewLifecycleOwner(), adapter::setNameStyle);
        viewModel.timetable().observe(getViewLifecycleOwner(), loaded -> {
            timetable = loaded;
            adapter.setTimetable(loaded);
            askForNotificationsOnce(loaded);
            var noSemesters = loaded.semesters().isEmpty();
            empty.setVisibility(noSemesters ? View.VISIBLE : View.GONE);
            pager.setVisibility(noSemesters ? View.INVISIBLE : View.VISIBLE);
            addCourse.setVisibility(noSemesters ? View.GONE : View.VISIBLE);
            openCourses.setVisibility(noSemesters ? View.GONE : View.VISIBLE);
            updateHeader(pager.getCurrentItem());
        });
        updateHeader(pager.getCurrentItem());
    }

    /**
     * Shows the zoom in percent, with a magnifier, at the bottom right of the grid. It stays while the grid is
     * zoomed, so a tap can reset the zoom, and fades out a moment after the zoom is back at 100 %.
     */
    private void showZoom(float zoom) {
        var percent = NumberFormat.getPercentInstance().format(zoom);
        zoomIndicator.setText(percent);
        zoomIndicator.setContentDescription(getString(R.string.zoom_reset_description, percent));
        zoomIndicator.removeCallbacks(hideZoom);
        zoomIndicator.animate().cancel();
        zoomIndicator.setVisibility(View.VISIBLE);
        zoomIndicator.setAlpha(1f);
        if (zoom <= ZoomMetrics.MIN) zoomIndicator.postDelayed(hideZoom, ZOOM_INDICATOR_MS);
    }

    /** Resets the zoom of the week that is shown; the others follow once it has settled. */
    private void resetZoom() {
        var weeks = (RecyclerView) pager.getChildAt(0);
        var holder = weeks.findViewHolderForAdapterPosition(pager.getCurrentItem());
        if (holder instanceof WeekPagerAdapter.PageHolder) ((WeekPagerAdapter.PageHolder) holder).page.resetZoom();
    }

    private void fadeOutZoom() {
        zoomIndicator.animate().alpha(0f).setDuration(250)
                .withEndAction(() -> zoomIndicator.setVisibility(View.INVISIBLE));
    }

    /**
     * Asks once, with an explanation of its own before the system's question, as soon as there is a reminder that
     * would not come without notifications. A no, or a swipe away, is answered by the hints in the event form and
     * in the settings, not by asking again.
     */
    private void askForNotificationsOnce(Timetable loaded) {
        var settings = UniPensumApp.from(requireContext()).settings();
        if (!settings.remindersEnabled() || settings.notificationsAsked()
                || NotificationAccess.allowed(requireContext())
                || !loaded.hasReminderFrom(LocalDate.now())) {
            return;
        }
        settings.setNotificationsAsked();
        ConfirmSheet.show(
                getParentFragmentManager(), KEY_NOTIFICATIONS,
                getString(R.string.reminder_permission_title), getString(R.string.reminder_permission_message),
                getString(R.string.reminder_permission_allow), false
        );
    }

    private void allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Before Android 13 the notifications are on until the user turns them off in the settings.
            NotificationAccess.openSettings(requireContext());
        }
    }

    /** The semester of the visible week, or the latest one if the week lies outside every semester. */
    @Nullable
    private Semester currentSemester() {
        if (timetable == null || timetable.semesters().isEmpty()) return null;
        var semester = semesterOfWeek(Weeks.mondayOf(pager.getCurrentItem()));
        if (semester != null) return semester;
        var all = timetable.semesters();
        return all.get(all.size() - 1);
    }

    private void openNewCourse() {
        var semester = currentSemester();
        if (semester == null) return;
        new ViewModelProvider(requireActivity()).get(CourseDraftViewModel.class).startNew(semester);
        Navigator.of(this).open(new CourseEditorFragment());
    }

    private void openCourses() {
        var semester = currentSemester();
        if (semester == null) return;
        Navigator.of(this).toggle(CoursesFragment.forSemester(semester.id()));
    }

    private void updateHeader(int position) {
        var monday = Weeks.mondayOf(position);
        var friday = monday.plusDays(4);

        var semester = semesterOfWeek(monday);
        if (semester != null) {
            title.setText(SemesterNames.display(requireContext(), semester));
        } else {
            title.setText(
                    timetable == null || timetable.semesters().isEmpty()
                            ? getString(R.string.app_name) : getString(R.string.no_semester)
            );
        }
        subtitle.setText(
                getString(
                        R.string.week_subtitle,
                        TimeFormat.dateShort(requireContext(), monday),
                        TimeFormat.dateShort(requireContext(), friday),
                        Weeks.isoWeekNumber(monday)
                )
        );
    }

    /** The first semester that holds a day of the week, which decides what the title says at a boundary. */
    @Nullable
    private Semester semesterOfWeek(LocalDate monday) {
        if (timetable == null) return null;
        for (var i = 0; i < 5; i++) {
            var semester = timetable.semesterAt(monday.plusDays(i));
            if (semester != null) return semester;
        }
        return null;
    }
}