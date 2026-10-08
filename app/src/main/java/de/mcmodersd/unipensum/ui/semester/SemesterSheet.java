package de.mcmodersd.unipensum.ui.semester;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.ui.format.SemesterNames;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;

/**
 * Lists all semesters. Tapping one jumps the week view to its first week (result under
 * {@link #REQUEST_JUMP}); the pencil edits it; the button at the bottom creates a new one.
 */
public final class SemesterSheet extends UpSheet {

    public static final String REQUEST_JUMP = "semester_jump";
    public static final String RESULT_EPOCH_DAY = "epochDay";

    private LinearLayout list;
    private List<Semester> current = new ArrayList<>();

    public static void show(FragmentManager manager) {
        new SemesterSheet().show(manager, "semesters");
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(R.string.semesters_title);
    }

    @Override
    protected View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;

        var column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);

        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        column.addView(
                list, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        var create = new UpButton(context);
        create.setText(R.string.semester_new);
        create.setOnClickListener(v -> {
            SemesterEditorSheet.show(getParentFragmentManager(), null, current);
        });
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = Math.round(12 * dp);
        column.addView(create, params);
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UniPensumApp.from(requireContext()).repository().semesters()
                .observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Semester> semesters) {
        current = semesters;
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        list.removeAllViews();

        if (semesters.isEmpty()) {
            var empty = new TextView(context);
            empty.setText(R.string.semesters_empty);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            empty.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            empty.setPadding(Math.round(8 * dp), Math.round(4 * dp), 0, Math.round(4 * dp));
            list.addView(empty);
            return;
        }

        var newestFirst = new ArrayList<Semester>(semesters);
        Collections.sort(newestFirst, Comparator.comparing(Semester::start).reversed());
        for (var semester : newestFirst) {
            var row = new UpRow(context);
            row.setTitle(SemesterNames.display(context, semester));
            row.setSubtitle(
                    TimeFormat.dateMedium(context, semester.start()) + " – "
                            + TimeFormat.dateMedium(context, semester.end())
            );
            row.setAction(R.drawable.ic_edit, getString(R.string.action_edit), v ->
                    SemesterEditorSheet.show(getParentFragmentManager(), semester, semesters)
            );
            row.setOnClickListener(v -> {
                var result = new Bundle();
                result.putLong(RESULT_EPOCH_DAY, semester.start().toEpochDay());
                getParentFragmentManager().setFragmentResult(REQUEST_JUMP, result);
                dismiss();
            });
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = Math.round(8 * dp);
            list.addView(row, params);
        }
    }
}