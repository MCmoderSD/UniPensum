package de.mcmodersd.unipensum.ui.lecturer;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;
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
        Bundle args = new Bundle();
        if (existing != null) {
            args.putLong(ARG_ID, existing.id());
            args.putString(ARG_FIRST_NAME, existing.firstName());
            args.putString(ARG_LAST_NAME, existing.lastName());
            args.putString(ARG_EMAIL, existing.email());
            args.putString(ARG_PHONE, existing.phone());
        }
        args.putString(ARG_RESULT_KEY, resultKey);
        LecturerEditorSheet sheet = new LecturerEditorSheet();
        sheet.setArguments(args);
        sheet.show(manager, "lecturer-editor");
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(requireArguments().getLong(ARG_ID) == 0 ? R.string.lecturer_new : R.string.lecturer_edit);
    }

    @Override
    protected View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        float dp = getResources().getDisplayMetrics().density;
        repository = UniPensumApp.from(context).repository();
        id = requireArguments().getLong(ARG_ID);
        // A recreated sheet gets back what was typed, not what the lecturer looked like when it opened.
        Bundle source = savedInstanceState != null ? savedInstanceState : requireArguments();

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);

        firstNameField = field(context, R.string.lecturer_first_name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                TextSanitizer.MAX_NAME, source.getString(ARG_FIRST_NAME));
        column.addView(firstNameField, spaced(dp, 0, 12));

        lastNameField = field(context, R.string.lecturer_last_name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                TextSanitizer.MAX_NAME, source.getString(ARG_LAST_NAME));
        column.addView(lastNameField, spaced(dp, 0, 12));

        emailField = field(context, R.string.lecturer_email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                TextSanitizer.MAX_EMAIL, source.getString(ARG_EMAIL));
        column.addView(emailField, spaced(dp, 0, 12));

        phoneField = field(context, R.string.lecturer_phone, InputType.TYPE_CLASS_PHONE,
                TextSanitizer.MAX_PHONE, source.getString(ARG_PHONE));
        column.addView(phoneField, spaced(dp, 0, 0));

        saveButton = new UpButton(context);
        saveButton.setText(R.string.action_save);
        saveButton.setOnClickListener(v -> onSave());
        column.addView(saveButton, spaced(dp, 16, 0));

        if (id != 0) {
            UpButton delete = new UpButton(context);
            delete.setText(R.string.action_delete);
            delete.setVariant(UpButton.Variant.DESTRUCTIVE);
            delete.setOnClickListener(v -> ConfirmSheet.show(getParentFragmentManager(), KEY_DELETE,
                    getString(R.string.lecturer_delete_title), getString(R.string.lecturer_delete_message),
                    getString(R.string.action_delete), true));
            column.addView(delete, spaced(dp, 8, 0));
        }
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(KEY_DELETE, getViewLifecycleOwner(),
                (key, result) -> delete());
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
        UpTextField field = new UpTextField(context);
        field.setLabel(getString(label));
        field.setInputType(inputType);
        field.setMaxLength(maxLength);
        if (text != null) field.setText(text);
        field.addTextWatcher(ignored -> field.setError(null));
        return field;
    }

    private void onSave() {
        Lecturer candidate = new Lecturer(id, firstNameField.getText(), lastNameField.getText(),
                emailField.getText(), phoneField.getText()).normalized();

        boolean valid = true;
        if (candidate.lastName().isEmpty()) {
            lastNameField.setError(getString(R.string.lecturer_error_last_name));
            valid = false;
        }
        if (candidate.email() != null && !Patterns.EMAIL_ADDRESS.matcher(candidate.email()).matches()) {
            emailField.setError(getString(R.string.lecturer_error_email));
            valid = false;
        }
        if (!valid) {
            Haptics.reject(saveButton);
            return;
        }

        saveButton.setEnabled(false);
        repository.saveLecturer(candidate, new Database.Callback<Long>() {
            @Override
            public void onSuccess(Long savedId) {
                if (!isAdded()) return;
                Haptics.confirm(saveButton);
                String resultKey = requireArguments().getString(ARG_RESULT_KEY);
                if (resultKey != null) {
                    Bundle result = new Bundle();
                    result.putLong(RESULT_LECTURER_ID, savedId);
                    getParentFragmentManager().setFragmentResult(resultKey, result);
                }
                dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                saveButton.setEnabled(true);
                lastNameField.setError(getString(R.string.lecturer_error_save));
                Haptics.reject(saveButton);
            }
        });
    }

    private void delete() {
        repository.deleteLecturer(id, new Database.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) lastNameField.setError(getString(R.string.lecturer_error_save));
            }
        });
    }

    private static LinearLayout.LayoutParams spaced(float dp, int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(topDp * dp);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}
