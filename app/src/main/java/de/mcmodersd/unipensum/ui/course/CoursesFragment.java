package de.mcmodersd.unipensum.ui.course;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.CourseContext;
import de.mcmodersd.unipensum.data.CourseWithSeries;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.format.CourseColors;
import de.mcmodersd.unipensum.ui.format.SemesterNames;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpRow;

/** All courses of one semester; tapping one opens it in the course editor. */
public class CoursesFragment extends Fragment {

    private static final String ARG_SEMESTER_ID = "semester_id";

    public static CoursesFragment forSemester(long semesterId) {
        var args = new Bundle();
        args.putLong(ARG_SEMESTER_ID, semesterId);
        var fragment = new CoursesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private TimetableRepository repository;
    private LinearLayout list;
    private View emptyView;
    private CourseDraftViewModel draft;
    private Semester semester;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_courses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = UniPensumApp.from(requireContext()).repository();
        draft = new ViewModelProvider(requireActivity()).get(CourseDraftViewModel.class);
        list = view.findViewById(R.id.courses);
        emptyView = view.findViewById(R.id.courses_empty);

        TextView title = view.findViewById(R.id.bar_title);
        title.setText(R.string.courses_title);
        view.findViewById(R.id.back).setOnClickListener(v -> Navigator.of(this).pop());

        var semesterId = requireArguments().getLong(ARG_SEMESTER_ID);
        repository.loadSemester(semesterId, new Database.Callback<Semester>() {
            @Override
            public void onSuccess(Semester loaded) {
                if (getView() == null) return;
                semester = loaded;
                title.setText(SemesterNames.display(requireContext(), loaded));
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) Navigator.of(CoursesFragment.this).pop();
            }
        });

        view.findViewById(R.id.new_course).setOnClickListener(v -> {
            if (semester == null) return;
            draft.startNew(semester);
            Navigator.of(this).push(new CourseEditorFragment());
        });
        // With the calendar beside this page, its semester can be deleted while the page is open.
        repository.semesters().observe(getViewLifecycleOwner(), semesters -> {
            for (var existing : semesters) {
                if (existing.id() == semesterId) return;
            }
            Navigator.of(this).pop();
        });
        repository.courses(semesterId).observe(getViewLifecycleOwner(), courses -> {
            list.removeAllViews();
            emptyView.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
            for (var entry : courses) list.addView(row(entry));
        });
    }

    private View row(CourseWithSeries entry) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var row = new UpRow(context);
        row.setTitle(entry.course().name());
        row.setSubtitle(
                getResources().getQuantityString(
                        R.plurals.event_count,
                        entry.series().size(), entry.series().size()
                )
        );
        row.setLeadingColor(CourseColors.resolve(context, entry.course().color()));
        row.setChevronVisible(true);
        row.setOnClickListener(v -> repository.loadCourse(entry.course().id(), new Database.Callback<CourseContext>() {
            @Override
            public void onSuccess(CourseContext loaded) {
                if (!isAdded()) return;
                draft.startEdit(loaded);
                Navigator.of(CoursesFragment.this).push(new CourseEditorFragment());
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) Haptics.reject(v);
            }
        }));
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = Math.round(8 * dp);
        row.setLayoutParams(params);
        return row;
    }
}
