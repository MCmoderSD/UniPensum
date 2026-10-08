package de.mcmodersd.unipensum.ui.course;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.model.Course;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.session.SessionEditorFragment;
import de.mcmodersd.unipensum.ui.widget.ColorSwatchPicker;
import de.mcmodersd.unipensum.ui.widget.ConfirmSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/**
 * Creates or edits a course: name, color and its events. Works on the shared
 * {@link CourseDraftViewModel}; nothing is stored before Save.
 */
public class CourseEditorFragment extends Fragment {

    private static final String KEY_DELETE = "course_delete";

    private CourseDraftViewModel draft;
    private TimetableRepository repository;
    private final HashMap<Long, Lecturer> lecturers = new HashMap<>();
    private NameStyle nameStyle = NameStyle.LAST_NAME;
    private UpTextField nameField;
    private UpTextField moodleField;
    private LinearLayout events;
    private TextView eventsError;
    private UpButton saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CourseDraftViewModel.class);
        if (!draft.isReady()) {
            // The process was restored without the draft: there is nothing to edit.
            view.post(() -> Navigator.of(this).pop());
            return;
        }
        repository = UniPensumApp.from(requireContext()).repository();

        ((TextView) view.findViewById(R.id.bar_title))
                .setText(draft.isNew() ? R.string.course_new : R.string.course_edit);
        view.findViewById(R.id.back).setOnClickListener(v -> Navigator.of(this).pop());

        nameField = view.findViewById(R.id.name_field);
        nameField.setMaxLength(TextSanitizer.MAX_NAME);
        nameField.setText(draft.name());
        nameField.addTextWatcher(text -> {
            draft.setName(text);
            nameField.setError(null);
        });

        moodleField = view.findViewById(R.id.moodle_field);
        moodleField.setMaxLength(TextSanitizer.MAX_LINK);
        moodleField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        moodleField.setText(draft.moodleLink());
        moodleField.addTextWatcher(text -> {
            draft.setMoodleLink(text);
            moodleField.setError(null);
        });

        ColorSwatchPicker colors = view.findViewById(R.id.color_picker);
        colors.setSelectedColor(draft.color());
        colors.setOnColorSelectedListener(draft::setColor);

        events = view.findViewById(R.id.events);
        eventsError = view.findViewById(R.id.events_error);
        view.findViewById(R.id.add_event).setOnClickListener(v ->
                Navigator.of(this).push(SessionEditorFragment.forSeries(-1))
        );
        renderEvents();
        // The event rows name their lecturer, so they follow changes to the lecturers and to the name style.
        repository.lecturers().observe(getViewLifecycleOwner(), list -> {
            lecturers.clear();
            for (var lecturer : list) lecturers.put(lecturer.id(), lecturer);
            renderEvents();
        });
        UniPensumApp.from(requireContext()).settings().lecturerNameStyle()
                .observe(getViewLifecycleOwner(), style -> {
                    nameStyle = style;
                    renderEvents();
                });

        // With the calendar beside this page, the semester of the course can be deleted, or all data replaced
        // by an import, while the editor is open. Then there is nothing left to save the course into.
        var semesterId = draft.semester().id();
        repository.semesters().observe(getViewLifecycleOwner(), semesters -> {
            for (var existing : semesters) {
                if (existing.id() == semesterId) return;
            }
            Navigator.of(this).pop();
        });

        saveButton = view.findViewById(R.id.save);
        saveButton.setOnClickListener(v -> save());

        UpButton delete = view.findViewById(R.id.delete);
        delete.setVisibility(draft.isNew() ? View.GONE : View.VISIBLE);
        delete.setOnClickListener(v -> ConfirmSheet.show(
                getParentFragmentManager(), KEY_DELETE,
                getString(R.string.course_delete_title), getString(R.string.course_delete_message),
                getString(R.string.action_delete), true
        ));
        getParentFragmentManager().setFragmentResultListener(
                KEY_DELETE, getViewLifecycleOwner(),
                (key, result) -> deleteCourse()
        );
    }

    private void renderEvents() {
        events.removeAllViews();
        eventsError.setVisibility(View.GONE);
        var dp = getResources().getDisplayMetrics().density;
        var series = draft.series();
        for (var i = 0; i < series.size(); i++) {
            final var index = i;
            var entry = series.get(i);
            var row = new UpRow(requireContext());
            row.setTitle(SeriesFormat.title(requireContext(), entry));
            var lecturer = lecturers.get(entry.details().lecturerId());
            row.setSubtitle(
                    SeriesFormat.subtitle(
                            requireContext(), entry,
                            lecturer == null ? null : lecturer.name(nameStyle)
                    )
            );
            row.setAction(R.drawable.ic_close, getString(R.string.event_remove), v -> {
                draft.series().remove(index);
                renderEvents();
            });
            row.setOnClickListener(v -> Navigator.of(this).push(SessionEditorFragment.forSeries(index)));
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = Math.round(8 * dp);
            events.addView(row, params);
        }
    }

    private void save() {
        // What is stored is the cleaned text, so a name of only blanks or invisible characters counts as empty.
        var name = TextSanitizer.line(draft.name(), TextSanitizer.MAX_NAME);
        var valid = true;
        if (name.isEmpty()) {
            nameField.setError(getString(R.string.course_error_name));
            valid = false;
        }
        if (!TextSanitizer.isValidWebLink(draft.moodleLink())) {
            moodleField.setError(getString(R.string.error_link));
            valid = false;
        }
        if (draft.isNew() && draft.series().isEmpty()) {
            eventsError.setText(R.string.course_error_events);
            eventsError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) {
            Haptics.reject(saveButton);
            return;
        }

        saveButton.setEnabled(false);
        var course = new Course(
                draft.courseId(), draft.semester().id(), name, draft.color(),
                draft.moodleLink()
        );
        // The database thread gets its own copy, the draft stays editable while it works.
        var snapshot = new ArrayList<>(draft.series());
        if (draft.isNew()) {
            repository.createCourse(course, snapshot, new Done<>());
        } else {
            repository.saveCourse(course, snapshot, new Done<>());
        }
    }

    private void deleteCourse() {
        repository.deleteCourse(draft.courseId(), new Done<>());
    }

    /** Leaves the screen on success, re-enables Save and says so on failure. */
    private final class Done<T> implements Database.Callback<T> {
        @Override
        public void onSuccess(T result) {
            if (!isAdded()) return;
            Haptics.confirm(saveButton);
            Navigator.of(CourseEditorFragment.this).pop();
        }

        @Override
        public void onError(Exception error) {
            if (!isAdded() || getView() == null) return;
            saveButton.setEnabled(true);
            eventsError.setText(R.string.course_error_save);
            eventsError.setVisibility(View.VISIBLE);
            Haptics.reject(saveButton);
        }
    }
}