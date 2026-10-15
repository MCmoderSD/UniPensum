package de.mcmodersd.unipensum.ui.semester;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.View.*;
import static android.view.ViewGroup.LayoutParams.*;
import static android.widget.LinearLayout.VERTICAL;
import static de.mcmodersd.unipensum.R.color.danger;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.domain.text.TextSanitizer.MAX_NAME;
import static de.mcmodersd.unipensum.ui.widget.DatePickerSheet.RESULT_EPOCH_DAY;
import static de.mcmodersd.unipensum.ui.widget.UpButton.Variant.DESTRUCTIVE;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.logic.SemesterDefaults;
import de.mcmodersd.unipensum.domain.logic.SemesterRules;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;
import de.mcmodersd.unipensum.ui.format.SemesterNames;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.ConfirmSheet;
import de.mcmodersd.unipensum.ui.widget.DatePickerSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/** Creates a semester or edits an existing one; also the place to delete it. */
public final class SemesterEditorSheet extends UpSheet {

    private static final String ARG_ID = "id";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_NAME = "name";

    private static final String STATE_START = "state_start";
    private static final String STATE_END = "state_end";
    private static final String STATE_NAME = "state_name";
    private static final String STATE_END_TOUCHED = "state_end_touched";

    private static final String KEY_START = "semester_start";
    private static final String KEY_END = "semester_end";
    private static final String KEY_SHORTEN = "semester_shorten";
    private static final String KEY_DELETE = "semester_delete";

    private final ArrayList<Semester> others = new ArrayList<>();
    private TimetableRepository repository;
    private long id;
    private Semester original;
    private LocalDate start;
    private LocalDate end;
    /**
     * Whether the end was chosen by hand. Until then, a new semester ends 16 weeks after the start the user
     * picks. A semester that already exists never moves its end by itself.
     */
    private boolean endTouched;
    private UpRow startRow;
    private UpRow endRow;
    private UpTextField nameField;
    private TextView errorView;
    private UpButton saveButton;

    /**
     * @param existing the semester to edit, or {@code null} to create one with a suggested period
     * @param all      all semesters, used to suggest a free period and to check for overlaps
     */
    public static void show(FragmentManager manager, @Nullable Semester existing, List<Semester> all) {
        var args = new Bundle();
        if (existing == null) {
            var period = SemesterDefaults.suggest(LocalDate.now(), all);
            args.putLong(ARG_ID, 0);
            args.putLong(ARG_START, period.start().toEpochDay());
            args.putLong(ARG_END, period.end().toEpochDay());
        } else {
            args.putLong(ARG_ID, existing.id());
            args.putLong(ARG_START, existing.start().toEpochDay());
            args.putLong(ARG_END, existing.end().toEpochDay());
            args.putString(ARG_NAME, existing.customName());
        }
        var sheet = new SemesterEditorSheet();
        sheet.setArguments(args);
        sheet.show(manager, "semester-editor");
    }

    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(requireArguments().getLong(ARG_ID) == 0 ? semester_new : semester_edit);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var args = requireArguments();
        var dp = getResources().getDisplayMetrics().density;
        repository = UniPensumApp.from(context).repository();

        id = args.getLong(ARG_ID);
        var source = savedInstanceState != null ? savedInstanceState : args;
        start = LocalDate.ofEpochDay(source.getLong(savedInstanceState != null ? STATE_START : ARG_START));
        end = LocalDate.ofEpochDay(source.getLong(savedInstanceState != null ? STATE_END : ARG_END));
        var name = source.getString(savedInstanceState != null ? STATE_NAME : ARG_NAME);
        endTouched = savedInstanceState != null && savedInstanceState.getBoolean(STATE_END_TOUCHED);
        original = id == 0 ? null : new Semester(
                id, LocalDate.ofEpochDay(args.getLong(ARG_START)),
                LocalDate.ofEpochDay(args.getLong(ARG_END)), args.getString(ARG_NAME)
        );

        var column = new LinearLayout(context);
        column.setOrientation(VERTICAL);

        startRow = new UpRow(context);
        startRow.setTitle(getString(semester_start));
        startRow.setChevronVisible(true);
        startRow.setOnClickListener(v -> DatePickerSheet.show(
                getParentFragmentManager(), KEY_START,
                getString(semester_start), start, null, null, false
        ));
        column.addView(startRow, spaced(dp, 8));

        endRow = new UpRow(context);
        endRow.setTitle(getString(semester_end));
        endRow.setChevronVisible(true);
        endRow.setOnClickListener(v -> DatePickerSheet.show(
                getParentFragmentManager(), KEY_END,
                getString(semester_end), end, null, null, false
        ));
        column.addView(endRow, spaced(dp, 16));

        nameField = new UpTextField(context);
        nameField.setLabel(getString(semester_name_label));
        nameField.setMaxLength(MAX_NAME);
        if (name != null) nameField.setText(name);
        column.addView(nameField, spaced(dp, 8));

        errorView = new TextView(context);
        errorView.setTextSize(COMPLEX_UNIT_SP, 14);
        errorView.setTextColor(ContextCompat.getColor(context, danger));
        errorView.setPadding(Math.round(4 * dp), Math.round(4 * dp), 0, Math.round(4 * dp));
        errorView.setVisibility(GONE);
        column.addView(errorView);

        saveButton = new UpButton(context);
        saveButton.setText(action_save);
        saveButton.setOnClickListener(v -> onSave());
        var saveParams = spaced(dp, 0);
        saveParams.topMargin = Math.round(12 * dp);
        column.addView(saveButton, saveParams);

        if (id != 0) {
            var delete = new UpButton(context);
            delete.setText(action_delete);
            delete.setVariant(DESTRUCTIVE);
            delete.setOnClickListener(v -> ConfirmSheet.show(
                    getParentFragmentManager(), KEY_DELETE,
                    getString(semester_delete_title), getString(semester_delete_message),
                    getString(action_delete), true
            ));
            var deleteParams = spaced(dp, 0);
            deleteParams.topMargin = Math.round(8 * dp);
            column.addView(delete, deleteParams);
        }

        refresh();
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        var manager = getParentFragmentManager();
        manager.setFragmentResultListener(KEY_START, getViewLifecycleOwner(), (key, result) -> {
            start = LocalDate.ofEpochDay(result.getLong(RESULT_EPOCH_DAY));
            if (id == 0 && !endTouched) end = SemesterDefaults.lectureEnd(start);
            refresh();
        });
        manager.setFragmentResultListener(KEY_END, getViewLifecycleOwner(), (key, result) -> {
            end = LocalDate.ofEpochDay(result.getLong(RESULT_EPOCH_DAY));
            endTouched = true;
            refresh();
        });
        manager.setFragmentResultListener(KEY_SHORTEN, getViewLifecycleOwner(), (key, result) -> save());
        manager.setFragmentResultListener(KEY_DELETE, getViewLifecycleOwner(), (key, result) -> delete());

        repository.semesters().observe(getViewLifecycleOwner(), semesters -> {
            others.clear();
            others.addAll(semesters);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_START, start.toEpochDay());
        outState.putLong(STATE_END, end.toEpochDay());
        outState.putString(STATE_NAME, nameField.getText());
        outState.putBoolean(STATE_END_TOUCHED, endTouched);
    }

    private void refresh() {
        var context = requireContext();
        startRow.setValue(TimeFormat.dateMedium(context, start));
        endRow.setValue(TimeFormat.dateMedium(context, end));
        nameField.setHint(SemesterNames.display(context, new Semester(0, start, end, null)));
        errorView.setVisibility(GONE);
    }

    private Semester candidate() {
        return new Semester(id, start, end, TextSanitizer.lineOrNull(nameField.getText(), MAX_NAME));
    }

    private void onSave() {
        var candidate = candidate();
        var error = switch (SemesterRules.check(candidate, others)) {
            case INVALID_RANGE -> getString(semester_error_range);
            case OVERLAPS_OTHER -> getString(semester_error_overlap, overlappingName(candidate));
            case OK -> null;
        };
        if (error != null) {
            showError(error);
            return;
        }
        var shortens = original != null
                && (start.isAfter(original.start()) || end.isBefore(original.end()));
        if (shortens) {
            ConfirmSheet.show(
                    getParentFragmentManager(), KEY_SHORTEN,
                    getString(semester_shorten_title), getString(semester_shorten_message),
                    getString(action_shorten), true
            );
        } else {
            save();
        }
    }

    private void save() {
        saveButton.setEnabled(false);
        repository.saveSemester(candidate(), new Database.Callback<>() {
            @Override
            public void onSuccess(Long result) {
                if (!isAdded()) return;
                Haptics.confirm(saveButton);
                dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                saveButton.setEnabled(true);
                showError(getString(semester_error_save));
            }
        });
    }

    private void delete() {
        repository.deleteSemester(id, new Database.Callback<>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) showError(getString(semester_error_save));
            }
        });
    }

    private String overlappingName(Semester candidate) {
        for (var other : others) {
            if (other.id() == candidate.id()) continue;
            if (!candidate.start().isAfter(other.end()) && !other.start().isAfter(candidate.end())) {
                return SemesterNames.display(requireContext(), other);
            }
        }
        return "";
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(VISIBLE);
        Haptics.reject(saveButton);
    }

    private static LinearLayout.LayoutParams spaced(float dp, int bottomDp) {
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}