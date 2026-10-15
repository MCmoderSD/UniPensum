package de.mcmodersd.unipensum.ui.lecturer;

import static android.text.InputType.*;
import static android.util.Patterns.EMAIL_ADDRESS;
import static android.view.ViewGroup.LayoutParams.*;
import static android.widget.LinearLayout.VERTICAL;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.domain.text.TextSanitizer.*;
import static de.mcmodersd.unipensum.ui.widget.UpButton.Variant.DESTRUCTIVE;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.ui.widget.ConfirmSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpSheet;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/** Creates a lecturer or edits an existing one; also the place to delete it. */
public final class LecturerEditorSheet extends UpSheet {

    /** Under the result key passed to {@link #show}: the id of the saved lecturer. */
    public static final String RESULT_LECTURER_ID = "lecturerId";

    private static final String ARG_ID = "id";
    private static final String ARG_FIRST_NAME = "first_name";
    private static final String ARG_LAST_NAME = "last_name";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_PHONE = "phone";
    private static final String ARG_RESULT_KEY = "result_key";

    private static final String KEY_DELETE = "lecturer_delete";

    private TimetableRepository repository;
    private long id;
    private UpTextField firstNameField;
    private UpTextField lastNameField;
    private UpTextField emailField;
    private UpTextField phoneField;
    private UpButton saveButton;

    /**
     * @param existing  the lecturer to edit, or {@code null} to create one
     * @param resultKey if not {@code null}, the saved lecturer's id is delivered under this key
     */
    public static void show(FragmentManager manager, @Nullable Lecturer existing, @Nullable String resultKey) {
        var args = new Bundle();
        if (existing != null) {
            args.putLong(ARG_ID, existing.id());
            args.putString(ARG_FIRST_NAME, existing.firstName());
            args.putString(ARG_LAST_NAME, existing.lastName());
            args.putString(ARG_EMAIL, existing.email());
            args.putString(ARG_PHONE, existing.phone());
        }
        args.putString(ARG_RESULT_KEY, resultKey);
        var sheet = new LecturerEditorSheet();
        sheet.setArguments(args);
        sheet.show(manager, "lecturer-editor");
    }

    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(requireArguments().getLong(ARG_ID) == 0 ? lecturer_new : lecturer_edit);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        repository = UniPensumApp.from(context).repository();
        id = requireArguments().getLong(ARG_ID);
        // A recreated sheet gets back what was typed, not what the lecturer looked like when it opened.
        var source = savedInstanceState != null ? savedInstanceState : requireArguments();

        var column = new LinearLayout(context);
        column.setOrientation(VERTICAL);

        firstNameField = field(
                context, lecturer_first_name,
                TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_CAP_WORDS | TYPE_TEXT_VARIATION_PERSON_NAME,
                MAX_NAME, source.getString(ARG_FIRST_NAME)
        );
        column.addView(firstNameField, spaced(dp, 0, 12));

        lastNameField = field(
                context, lecturer_last_name,
                TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_CAP_WORDS | TYPE_TEXT_VARIATION_PERSON_NAME,
                MAX_NAME, source.getString(ARG_LAST_NAME)
        );
        column.addView(lastNameField, spaced(dp, 0, 12));

        emailField = field(
                context, lecturer_email,
                TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                MAX_EMAIL, source.getString(ARG_EMAIL)
        );
        column.addView(emailField, spaced(dp, 0, 12));

        phoneField = field(context, lecturer_phone, TYPE_CLASS_PHONE, MAX_PHONE, source.getString(ARG_PHONE));
        column.addView(phoneField, spaced(dp, 0, 0));

        saveButton = new UpButton(context);
        saveButton.setText(action_save);
        saveButton.setOnClickListener(v -> onSave());
        column.addView(saveButton, spaced(dp, 16, 0));

        if (id != 0) {
            var delete = new UpButton(context);
            delete.setText(action_delete);
            delete.setVariant(DESTRUCTIVE);
            delete.setOnClickListener(v -> ConfirmSheet.show(
                    getParentFragmentManager(), KEY_DELETE,
                    getString(lecturer_delete_title), getString(lecturer_delete_message),
                    getString(action_delete), true
            ));
            column.addView(delete, spaced(dp, 8, 0));
        }
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(
                KEY_DELETE, getViewLifecycleOwner(),
                (key, result) -> delete()
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_FIRST_NAME, firstNameField.getText());
        outState.putString(ARG_LAST_NAME, lastNameField.getText());
        outState.putString(ARG_EMAIL, emailField.getText());
        outState.putString(ARG_PHONE, phoneField.getText());
    }

    private UpTextField field(Context context, int label, int inputType, int maxLength, @Nullable String text) {
        var field = new UpTextField(context);
        field.setLabel(getString(label));
        field.setInputType(inputType);
        field.setMaxLength(maxLength);
        if (text != null) field.setText(text);
        field.addTextWatcher(ignored -> field.setError(null));
        return field;
    }

    private void onSave() {
        var candidate = new Lecturer(
                id, firstNameField.getText(), lastNameField.getText(),
                emailField.getText(), phoneField.getText()
        ).normalized();

        var valid = true;
        if (candidate.lastName().isEmpty()) {
            lastNameField.setError(getString(lecturer_error_last_name));
            valid = false;
        }
        if (candidate.email() != null && !EMAIL_ADDRESS.matcher(candidate.email()).matches()) {
            emailField.setError(getString(lecturer_error_email));
            valid = false;
        }
        if (!valid) {
            Haptics.reject(saveButton);
            return;
        }

        saveButton.setEnabled(false);
        repository.saveLecturer(candidate, new Database.Callback<>() {
            @Override
            public void onSuccess(Long savedId) {
                if (!isAdded()) return;
                Haptics.confirm(saveButton);
                var resultKey = requireArguments().getString(ARG_RESULT_KEY);
                if (resultKey != null) {
                    var result = new Bundle();
                    result.putLong(RESULT_LECTURER_ID, savedId);
                    getParentFragmentManager().setFragmentResult(resultKey, result);
                }
                dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                saveButton.setEnabled(true);
                lastNameField.setError(getString(lecturer_error_save));
                Haptics.reject(saveButton);
            }
        });
    }

    private void delete() {
        repository.deleteLecturer(id, new Database.Callback<>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) lastNameField.setError(getString(lecturer_error_save));
            }
        });
    }

    private static LinearLayout.LayoutParams spaced(float dp, int topDp, int bottomDp) {
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = Math.round(topDp * dp);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}