package de.mcmodersd.unipensum.ui.lecturer;

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

import java.util.List;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;

/**
 * The list of lecturers, in two modes:
 * <ul>
 *   <li><b>manage</b> (from the settings): tapping a lecturer edits it;</li>
 *   <li><b>pick</b> (from the event form): tapping a lecturer chooses it and delivers its id under the
 *       request key ({@link #RESULT_LECTURER_ID}, {@link SessionDetails#NO_LECTURER} for "None"). A lecturer
 *       created from here is chosen right away.</li>
 * </ul>
 * Each row is just the name, kept plain on purpose: the contact details are in the editor and, for a
 * session, in its sheet. The list always shows full names; the name style of the settings only applies elsewhere.
 */
public final class LecturerSheet extends UpSheet {

    public static final String RESULT_LECTURER_ID = "lecturerId";

    private static final String ARG_PICK_KEY = "pick_key";
    private static final String ARG_SELECTED = "selected";
    private static final String KEY_CREATED = "lecturer_created";

    private LinearLayout list;

    public static void showManager(FragmentManager manager) {
        new LecturerSheet().show(manager, "lecturers");
    }

    /** @param selectedId the lecturer to mark as chosen, {@link SessionDetails#NO_LECTURER} for none */
    public static void showPicker(FragmentManager manager, String requestKey, long selectedId) {
        var args = new Bundle();
        args.putString(ARG_PICK_KEY, requestKey);
        args.putLong(ARG_SELECTED, selectedId);
        var sheet = new LecturerSheet();
        sheet.setArguments(args);
        sheet.show(manager, "lecturer-picker");
    }

    private boolean picking() {
        return pickKey() != null;
    }

    @Nullable
    private String pickKey() {
        var args = getArguments();
        return args == null ? null : args.getString(ARG_PICK_KEY);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(picking() ? R.string.lecturer_pick_title : R.string.lecturers_title);
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
        column.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        var create = new UpButton(context);
        create.setText(R.string.lecturer_new);
        create.setOnClickListener(v ->
                LecturerEditorSheet.show(getParentFragmentManager(), null, picking() ? KEY_CREATED : null));
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(12 * dp);
        column.addView(create, params);
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (picking()) {
            getParentFragmentManager().setFragmentResultListener(KEY_CREATED, getViewLifecycleOwner(),
                    (key, result) -> pick(result.getLong(LecturerEditorSheet.RESULT_LECTURER_ID)));
        }
        UniPensumApp.from(requireContext()).repository().lecturers()
                .observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Lecturer> lecturers) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var selected = picking() ? requireArguments().getLong(ARG_SELECTED) : SessionDetails.NO_LECTURER;
        list.removeAllViews();

        if (picking()) {
            var none = new UpRow(context);
            none.setTitle(getString(R.string.lecturer_none));
            if (selected == SessionDetails.NO_LECTURER) none.setTrailingIcon(R.drawable.ic_check);
            none.setOnClickListener(v -> pick(SessionDetails.NO_LECTURER));
            list.addView(none, rowParams(dp));
        }

        if (lecturers.isEmpty() && !picking()) {
            var empty = new TextView(context);
            empty.setText(R.string.lecturers_empty);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            empty.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            empty.setPadding(Math.round(8 * dp), Math.round(4 * dp), 0, Math.round(4 * dp));
            list.addView(empty);
            return;
        }

        for (var lecturer : lecturers) {
            var row = new UpRow(context);
            row.setTitle(lecturer.name(NameStyle.FULL_NAME));
            if (picking()) {
                if (lecturer.id() == selected) row.setTrailingIcon(R.drawable.ic_check);
                row.setOnClickListener(v -> pick(lecturer.id()));
            } else {
                row.setChevronVisible(true);
                row.setOnClickListener(v -> LecturerEditorSheet.show(getParentFragmentManager(), lecturer, null));
            }
            list.addView(row, rowParams(dp));
        }
    }

    private void pick(long lecturerId) {
        var result = new Bundle();
        result.putLong(RESULT_LECTURER_ID, lecturerId);
        getParentFragmentManager().setFragmentResult(requireArguments().getString(ARG_PICK_KEY), result);
        dismiss();
    }

    private static LinearLayout.LayoutParams rowParams(float dp) {
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = Math.round(8 * dp);
        return params;
    }
}
