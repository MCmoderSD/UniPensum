package de.mcmodersd.unipensum.ui.week;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import java.time.LocalDate;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.data.Timetable;
import de.mcmodersd.unipensum.domain.logic.Weeks;
import de.mcmodersd.unipensum.domain.model.Semester;
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

/** The app's one main screen: the semester name, the week range and the swipeable week grid. */
public class WeekFragment extends Fragment {

    private WeekViewModel viewModel;
    private ViewPager2 pager;
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

        viewModel = new ViewModelProvider(this).get(WeekViewModel.class);
        WeekPagerAdapter adapter = new WeekPagerAdapter();
        adapter.setOnSessionClickListener(session ->
                SessionDetailSheet.show(getParentFragmentManager(), session.session().id()));
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        // The view is rebuilt after an editor was closed; come back to the week that was open.
        pager.setCurrentItem(viewModel.page() >= 0 ? viewModel.page() : Weeks.initialPosition(LocalDate.now()), false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewModel.setPage(position);
                updateHeader(position);
            }
        });

        title.setOnClickListener(v -> SemesterSheet.show(getParentFragmentManager()));
        addCourse.setOnClickListener(v -> openNewCourse());
        openCourses.setOnClickListener(v -> openCourses());
        view.findViewById(R.id.open_settings).setOnClickListener(v ->
                Navigator.of(this).push(new SettingsFragment()));
        view.findViewById(R.id.create_semester).setOnClickListener(v ->
                SemesterEditorSheet.show(getParentFragmentManager(), null, timetable.semesters()));
        getParentFragmentManager().setFragmentResultListener(SemesterSheet.REQUEST_JUMP, getViewLifecycleOwner(),
                (key, result) -> pager.setCurrentItem(
                        Weeks.positionOf(LocalDate.ofEpochDay(result.getLong(SemesterSheet.RESULT_EPOCH_DAY))), false));

        viewModel.timeWindow().observe(getViewLifecycleOwner(), adapter::setWindow);
        viewModel.lecturerNameStyle().observe(getViewLifecycleOwner(), adapter::setNameStyle);
        viewModel.timetable().observe(getViewLifecycleOwner(), loaded -> {
            timetable = loaded;
            adapter.setTimetable(loaded);
            boolean noSemesters = loaded.semesters().isEmpty();
            empty.setVisibility(noSemesters ? View.VISIBLE : View.GONE);
            pager.setVisibility(noSemesters ? View.INVISIBLE : View.VISIBLE);
            addCourse.setVisibility(noSemesters ? View.GONE : View.VISIBLE);
            openCourses.setVisibility(noSemesters ? View.GONE : View.VISIBLE);
            updateHeader(pager.getCurrentItem());
        });
        updateHeader(pager.getCurrentItem());
    }

    /** The semester of the visible week, or the latest one if the week lies outside every semester. */
    @Nullable
    private Semester currentSemester() {
        if (timetable == null || timetable.semesters().isEmpty()) return null;
        Semester semester = semesterOfWeek(Weeks.mondayOf(pager.getCurrentItem()));
        if (semester != null) return semester;
        List<Semester> all = timetable.semesters();
        return all.get(all.size() - 1);
    }

    private void openNewCourse() {
        Semester semester = currentSemester();
        if (semester == null) return;
        new ViewModelProvider(requireActivity()).get(CourseDraftViewModel.class).startNew(semester);
        Navigator.of(this).push(new CourseEditorFragment());
    }

    private void openCourses() {
        Semester semester = currentSemester();
        if (semester == null) return;
        Navigator.of(this).push(CoursesFragment.forSemester(semester.id()));
    }

    private void updateHeader(int position) {
        LocalDate monday = Weeks.mondayOf(position);
        LocalDate friday = monday.plusDays(4);

        Semester semester = semesterOfWeek(monday);
        if (semester != null) {
            title.setText(SemesterNames.display(requireContext(), semester));
        } else {
            title.setText(timetable == null || timetable.semesters().isEmpty()
                    ? getString(R.string.app_name) : getString(R.string.no_semester));
        }
        subtitle.setText(getString(R.string.week_subtitle,
                TimeFormat.dateShort(requireContext(), monday),
                TimeFormat.dateShort(requireContext(), friday),
                Weeks.isoWeekNumber(monday)));
    }

    /** The first semester that holds a day of the week, which decides what the title says at a boundary. */
    @Nullable
    private Semester semesterOfWeek(LocalDate monday) {
        if (timetable == null) return null;
        for (int i = 0; i < 5; i++) {
            Semester semester = timetable.semesterAt(monday.plusDays(i));
            if (semester != null) return semester;
        }
        return null;
    }
}
