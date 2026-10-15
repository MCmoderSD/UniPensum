package de.mcmodersd.unipensum.ui.backup;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.View.*;
import static android.view.ViewGroup.LayoutParams.*;
import static android.widget.LinearLayout.VERTICAL;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.plurals.backup_error_password_short;
import static de.mcmodersd.unipensum.R.string.*;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.util.Arrays;

import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpSheet;
import de.mcmodersd.unipensum.ui.widget.UpSwitch;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/**
 * Saves all data to a {@code backup.unipensum} file, optionally protected with a password. Where the file
 * goes is chosen in the system's own dialog, which needs no permission.
 * <p>
 * The password is only kept in memory between tapping Export and the dialog's answer. If the sheet is
 * recreated in between (the system may do that), it is gone, and then nothing is written: a backup the
 * user asked to protect must never be written unprotected.
 */
public final class BackupExportSheet extends UpSheet {

    private static final String FILE_NAME = "backup.unipensum";
    /** A short password is quickly guessed, even with the slow key derivation. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final ActivityResultLauncher<String> createDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"), this::onTargetChosen
    );

    private View form;
    private UpSwitch protect;
    private UpTextField password;
    private UpTextField repeat;
    private UpButton exportButton;
    private TextView status;
    private UpButton close;

    /** Set while this instance has the system dialog open, so an answer meant for another instance is ignored. */
    private boolean awaitingTarget;
    private char[] pendingPassword;

    public static void show(FragmentManager manager) {
        new BackupExportSheet().show(manager, "backup-export");
    }

    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(backup_export_title);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;

        var column = new LinearLayout(context);
        column.setOrientation(VERTICAL);

        // The form: explanation, password switch, password fields, button.
        var formColumn = new LinearLayout(context);
        formColumn.setOrientation(VERTICAL);
        form = formColumn;

        formColumn.addView(text(context, backup_export_description, 16, text_secondary), spaced(dp, 0, 16));

        var switchRow = new LinearLayout(context);
        switchRow.setGravity(CENTER_VERTICAL);
        switchRow.setMinimumHeight(Math.round(48 * dp));
        switchRow.setPadding(Math.round(4 * dp), 0, Math.round(4 * dp), 0);
        var switchLabel = text(context, backup_protect, 16, text_primary);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
        protect = new UpSwitch(context);
        protect.setContentDescription(getString(backup_protect));
        switchRow.addView(protect);
        formColumn.addView(switchRow, spaced(dp, 0, 8));

        var block = new LinearLayout(context);
        block.setOrientation(VERTICAL);
        block.setVisibility(GONE);
        password = passwordField(context, backup_password);
        block.addView(password, spaced(dp, 0, 12));
        repeat = passwordField(context, backup_password_repeat);
        block.addView(repeat, spaced(dp, 0, 8));
        var hint = text(context, backup_password_hint, 13, text_secondary);
        hint.setPadding(Math.round(4 * dp), 0, Math.round(4 * dp), 0);
        block.addView(hint, spaced(dp, 0, 8));
        formColumn.addView(block, spaced(dp, 0, 0));
        protect.setOnCheckedChangeListener(checked -> block.setVisibility(checked ? VISIBLE : GONE));

        exportButton = new UpButton(context);
        exportButton.setText(backup_action_export);
        exportButton.setOnClickListener(v -> onExport());
        formColumn.addView(exportButton, spaced(dp, 12, 0));
        column.addView(formColumn);

        // The outcome: a message and a button, shown instead of the form.
        status = text(context, backup_exporting, 16, text_primary);
        status.setPadding(Math.round(4 * dp), Math.round(8 * dp), Math.round(4 * dp), Math.round(16 * dp));
        status.setVisibility(GONE);
        column.addView(status);
        close = new UpButton(context);
        close.setText(action_done);
        close.setOnClickListener(v -> dismiss());
        close.setVisibility(GONE);
        column.addView(close, spaced(dp, 0, 0));
        return column;
    }

    private UpTextField passwordField(Context context, @StringRes int label) {
        var field = new UpTextField(context);
        field.setLabel(getString(label));
        field.setPassword();
        field.addTextWatcher(ignored -> field.setError(null));
        return field;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clear(pendingPassword);
        pendingPassword = null;
    }

    private void onExport() {
        char[] chars = null;
        if (protect.isChecked()) {
            chars = password.getTextChars();
            var again = repeat.getTextChars();
            var valid = true;
            if (chars.length < MIN_PASSWORD_LENGTH) {
                password.setError(
                        getResources().getQuantityString(
                                backup_error_password_short,
                                MIN_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH
                        )
                );
                valid = false;
            }
            if (!Arrays.equals(chars, again)) {
                repeat.setError(getString(backup_error_password_mismatch));
                valid = false;
            }
            clear(again);
            if (!valid) {
                clear(chars);
                Haptics.reject(exportButton);
                return;
            }
        }
        pendingPassword = chars;
        awaitingTarget = true;
        exportButton.setEnabled(false);
        createDocument.launch(FILE_NAME);
    }

    /** The answer of the system dialog: where to save, or {@code null} if the user backed out. */
    private void onTargetChosen(@Nullable Uri target) {
        var mine = awaitingTarget;
        awaitingTarget = false;
        var chars = pendingPassword;
        pendingPassword = null;
        if (getView() == null) {
            clear(chars);
            return;
        }
        if (!mine) {
            clear(chars);
            if (target != null) showOutcome(backup_export_interrupted, true);
            return;
        }
        exportButton.setEnabled(true);
        if (target == null) {
            clear(chars);
            return;
        }

        form.setVisibility(GONE);
        status.setText(backup_exporting);
        status.setTextColor(ContextCompat.getColor(requireContext(), text_primary));
        status.setVisibility(VISIBLE);
        UniPensumApp.from(requireContext()).backups().export(target, chars, new Database.Callback<>() {
            @Override
            public void onSuccess(Void result) {
                if (getView() == null) return;
                Haptics.confirm(status);
                showOutcome(backup_exported, false);
            }

            @Override
            public void onError(Exception error) {
                if (getView() == null) return;
                Haptics.reject(status);
                showOutcome(backup_export_failed, true);
            }
        });
    }

    private void showOutcome(@StringRes int message, boolean error) {
        form.setVisibility(GONE);
        status.setText(message);
        status.setTextColor(ContextCompat.getColor(requireContext(), error ? danger : text_primary));
        status.setVisibility(VISIBLE);
        close.setVisibility(VISIBLE);
    }

    private TextView text(Context context, @StringRes int resource, float sp, int colorRes) {
        var view = new TextView(context);
        view.setText(resource);
        view.setTextSize(COMPLEX_UNIT_SP, sp);
        view.setTextColor(ContextCompat.getColor(context, colorRes));
        return view;
    }

    private static void clear(@Nullable char[] chars) {
        if (chars != null) Arrays.fill(chars, '\0');
    }

    private static LinearLayout.LayoutParams spaced(float dp, int topDp, int bottomDp) {
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = Math.round(topDp * dp);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}