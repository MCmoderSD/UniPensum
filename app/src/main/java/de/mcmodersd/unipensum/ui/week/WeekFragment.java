package de.mcmodersd.unipensum.ui.week;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.view.KeyEvent.*;
import static android.view.View.*;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static de.mcmodersd.unipensum.R.id.*;
import static de.mcmodersd.unipensum.R.layout.fragment_week;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.ui.semester.SemesterSheet.*;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.text.NumberFormat;
import java.time.LocalDate;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
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
import de.mcmodersd.unipensum.ui.widget.FocusRing;
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
        return inflater.inflate(fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        title = view.findViewById(R.id.title);
        subtitle = view.findViewById(R.id.subtitle);
        empty = view.findViewById(R.id.empty);
        addCourse = view.findViewById(add_course);
        openCourses = view.findViewById(open_courses);
        pager = view.findViewById(R.id.pager);
        zoomIndicator = view.findViewById(zoom_indicator);

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
        // The view is rebuilt while the grid is zoomed (turning, theme, language): the label has to come back. A list
        // of days, shown for large text, has no zoom.
        if (viewModel.zoom() > ZoomMetrics.MIN && !WeekAgendaView.wanted(requireContext())) {
            showZoom(viewModel.zoom());
        }
        adapter.setOnSessionClickListener(session ->
                SessionDetailSheet.show(getParentFragmentManager(), session.session().id())
        );
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        // The weeks are not a stop of their own: the Tab key goes on to the sessions, and Page Up and Page Down turn
        // the weeks, see turnWeek.
        weeks().setFocusable(false);
        ViewCompat.setAccessibilityDelegate(weeks(), new WeekPositionDelegate());
        ViewCompat.addOnUnhandledKeyEventListener(view, (root, event) -> turnWeek(root, event));
        // The view is rebuilt when the screen is recreated (turning, theme, language); come back to the same week.
        pager.setCurrentItem(viewModel.page() >= 0 ? viewModel.page() : Weeks.initialPosition(LocalDate.now()), false);
        adapter.setCurrentPage(pager.getCurrentItem());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewModel.setPage(position);
                adapter.setCurrentPage(position);
                updateHeader(position);
            }
        });

        getParentFragmentManager().setFragmentResultListener(
                KEY_NOTIFICATIONS, getViewLifecycleOwner(),
                (key, result) -> allowNotifications()
        );
        FocusRing.install(title, 8);
        title.setOnClickListener(v -> {
            Haptics.tap(v);
            SemesterSheet.show(getParentFragmentManager());
        });
        addCourse.setOnClickListener(v -> openNewCourse());
        openCourses.setOnClickListener(v -> openCourses());
        view.findViewById(open_settings).setOnClickListener(v ->
                Navigator.of(this).toggle(new SettingsFragment())
        );
        view.findViewById(create_semester).setOnClickListener(v ->
                SemesterEditorSheet.show(getParentFragmentManager(), null, timetable.semesters())
        );
        getParentFragmentManager().setFragmentResultListener(
                REQUEST_JUMP, getViewLifecycleOwner(),
                (key, result) -> pager.setCurrentItem(
                        Weeks.positionOf(LocalDate.ofEpochDay(result.getLong(RESULT_EPOCH_DAY))), false
                )
        );

        viewModel.timeWindow().observe(getViewLifecycleOwner(), adapter::setWindow);
        viewModel.lecturerNameStyle().observe(getViewLifecycleOwner(), adapter::setNameStyle);
        viewModel.timetable().observe(getViewLifecycleOwner(), loaded -> {
            timetable = loaded;
            adapter.setTimetable(loaded);
            askForNotificationsOnce(loaded);
            var noSemesters = loaded.semesters().isEmpty();
            empty.setVisibility(noSemesters ? VISIBLE : GONE);
            pager.setVisibility(noSemesters ? INVISIBLE : VISIBLE);
            addCourse.setVisibility(noSemesters ? GONE : VISIBLE);
            openCourses.setVisibility(noSemesters ? GONE : VISIBLE);
            updateHeader(pager.getCurrentItem());
        });
        updateHeader(pager.getCurrentItem());
    }

    /**
     * The pager says "Page 360 of 1560" when a week is turned, the place among every week there is, which means
     * nothing. The week itself is said by the subtitle, a live region, so that report is left out.
     */
    private static final class WeekPositionDelegate extends AccessibilityDelegateCompat {
        @Override
        public void onInitializeAccessibilityEvent(@NonNull View host, @NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            if (event.getEventType() != TYPE_VIEW_SCROLLED) return;
            event.setFromIndex(-1);
            event.setToIndex(-1);
            event.setItemCount(-1);
        }
    }

    /**
     * Page Up and Page Down show the week before and after, Home the week of today, for a keyboard that has the focus
     * in the calendar. The arrow keys stay with the focus, which they move between the sessions.
     *
     * @return whether the key was taken
     */
    private boolean turnWeek(View root, KeyEvent event) {
        var key = event.getKeyCode();
        if (key != KEYCODE_PAGE_UP && key != KEYCODE_PAGE_DOWN && key != KEYCODE_MOVE_HOME) return false;
        // A page beside the calendar, or over it, has its own keys.
        if (!root.hasFocus() || event.getRepeatCount() > 0) return false;
        if (event.getAction() == ACTION_DOWN) {
            var target = key == KEYCODE_MOVE_HOME
                    ? Weeks.initialPosition(LocalDate.now())
                    : pager.getCurrentItem() + (key == KEYCODE_PAGE_DOWN ? 1 : -1);
            var adapter = pager.getAdapter();
            if (adapter != null && target >= 0 && target < adapter.getItemCount()) pager.setCurrentItem(target, true);
        }
        return true;
    }

    /**
     * Shows the zoom in percent, with a magnifier, at the bottom right of the grid. It stays while the grid is
     * zoomed, so a tap can reset the zoom, and fades out a moment after the zoom is back at 100 %.
     */
    private void showZoom(float zoom) {
        var percent = NumberFormat.getPercentInstance().format(zoom);
        zoomIndicator.setText(percent);
        zoomIndicator.setContentDescription(getString(zoom_reset_description, percent));
        zoomIndicator.removeCallbacks(hideZoom);
        zoomIndicator.animate().cancel();
        zoomIndicator.setVisibility(VISIBLE);
        zoomIndicator.setAlpha(1f);
        if (zoom <= ZoomMetrics.MIN) zoomIndicator.postDelayed(hideZoom, ZOOM_INDICATOR_MS);
    }

    /** The list inside the pager that holds the weeks. ViewPager2 builds it as its only child and offers no other way in. */
    private RecyclerView weeks() {
        return (RecyclerView) pager.getChildAt(0);
    }

    /** Resets the zoom of the week that is shown; the others follow once it has settled. */
    private void resetZoom() {
        var holder = weeks().findViewHolderForAdapterPosition(pager.getCurrentItem());
        if (holder instanceof WeekPagerAdapter.PageHolder) ((WeekPagerAdapter.PageHolder) holder).page.resetZoom();
    }

    private void fadeOutZoom() {
        zoomIndicator.animate().alpha(0f).setDuration(250).withEndAction(() -> zoomIndicator.setVisibility(INVISIBLE));
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
                getString(reminder_permission_title), getString(reminder_permission_message),
                getString(reminder_permission_allow), false
        );
    }

    private void allowNotifications() {
        if (SDK_INT >= TIRAMISU) {
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
                            ? getString(app_name) : getString(no_semester)
            );
        }
        // The description replaces the text for a screen reader, so it has to say the name of the semester too.
        title.setContentDescription(getString(semester_switch_description, title.getText()));
        // A live region says the new week when the user turns it; the same text again would be said again.
        var range = getString(
                week_subtitle,
                TimeFormat.dateShort(requireContext(), monday),
                TimeFormat.dateShort(requireContext(), friday),
                Weeks.isoWeekNumber(monday)
        );
        if (!TextUtils.equals(subtitle.getText(), range)) subtitle.setText(range);
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