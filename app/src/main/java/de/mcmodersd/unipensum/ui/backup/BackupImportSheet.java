package de.mcmodersd.unipensum.ui.backup;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.ViewGroup.LayoutParams.*;
import static android.widget.LinearLayout.VERTICAL;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.plurals.*;
import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.domain.backup.BackupInfo.Compatibility.*;
import static de.mcmodersd.unipensum.ui.widget.UpButton.Variant.*;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.time.ZoneId;

import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/**
 * Imports a backup the user picked with the system's file dialog. It walks through the steps of
 * {@link BackupImportViewModel}: read the file, ask for the password if there is one, show what the file
 * holds (with a warning if it comes from another version), and only after an explicit confirmation
 * replace all data.
 */
@SuppressWarnings("SizeReplaceableByIsEmpty")
public final class BackupImportSheet extends UpSheet {

    private static final String ARG_SOURCE = "source";

    private BackupImportViewModel model;
    private LinearLayout column;

    public static void show(FragmentManager manager, Uri source) {
        var args = new Bundle();
        args.putString(ARG_SOURCE, source.toString());
        var sheet = new BackupImportSheet();
        sheet.setArguments(args);
        sheet.show(manager, "backup-import");
    }

    @Override
    protected CharSequence title(@NonNull Context context) {
        return context.getString(backup_import_title);
    }

    @Override
    protected View createContent(@Nullable Bundle savedInstanceState) {
        column = new LinearLayout(requireContext());
        column.setOrientation(VERTICAL);
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(this).get(BackupImportViewModel.class);
        model.start(Uri.parse(requireArguments().getString(ARG_SOURCE)));
        model.step().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(BackupImportViewModel.Step step) {
        column.removeAllViews();
        switch (step) {
            case READING:
            case LOADING:
                busy(backup_reading);
                break;
            case IMPORTING:
                busy(backup_importing);
                break;
            case PASSWORD:
                askForPassword();
                break;
            case REVIEW:
                review();
                break;
            case DONE:
                finished(backup_imported, false);
                break;
            case ERROR:
            default:
                finished(model.errorMessage(), true);
                break;
        }
    }

    // --- steps ---

    private void busy(@StringRes int message) {
        column.addView(text(message, 16, text_secondary, 8, 16));
    }

    private void askForPassword() {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;

        column.addView(text(backup_protected, 16, text_secondary, 0, 12));
        var password = new UpTextField(context);
        password.setLabel(getString(backup_password));
        password.setPassword();
        if (model.wrongPassword()) password.setError(getString(backup_error_wrong_password));
        password.addTextWatcher(ignored -> password.setError(null));
        column.addView(password, params(dp, 12));

        var next = new UpButton(context);
        next.setText(action_continue);
        next.setOnClickListener(v -> {
            var chars = password.getTextChars();
            if (chars.length == 0) {
                Haptics.reject(next);
                return;
            }
            model.submitPassword(chars);
        });
        column.addView(next, params(dp, 8));
        column.addView(cancelButton(), params(dp, 0));
    }

    private void review() {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var info = model.info();
        var report = model.report();
        if (info == null || report == null) {
            finished(backup_error_generic, true);
            return;
        }

        var origin = new StringBuilder();
        if (info.exportedAt() != null) {
            origin.append(
                    getString(
                            backup_created, TimeFormat.dateMedium(
                                    context,
                                    info.exportedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                            )
                    )
            );
        }
        if (!info.appVersion().isEmpty()) {
            if (origin.length() > 0) origin.append(" · ");
            origin.append(getString(backup_version, info.appVersion()));
        }
        if (origin.length() > 0) column.addView(plainText(origin.toString(), 14, text_secondary, 0, 12));

        var current = model.current();
        var compatibility =
                info.compatibility(current.format(), current.schema(), current.appVersionCode());
        if (compatibility != SAME) {
            column.addView(
                    text(compatibility == NEWER ? backup_warning_newer : backup_warning_older, 15, danger, 0, 12)
            );
        }

        column.addView(text(backup_contents, 13, text_secondary, 0, 6));
        column.addView(countRow(semesters_title, report.semesters()), params(dp, 8));
        column.addView(countRow(courses_title, report.courses()), params(dp, 8));
        column.addView(countRow(backup_sessions, report.sessions()), params(dp, 8));
        column.addView(countRow(lecturers_title, report.lecturers()), params(dp, 8));

        if (report.skipped() > 0) {
            column.addView(
                    plainText(
                            getResources().getQuantityString(
                                    backup_skipped,
                                    report.skipped(), report.skipped()
                            ), 14, danger, 4, 4
                    )
            );
        }
        if (report.adjusted() > 0) {
            column.addView(
                    plainText(
                            getResources().getQuantityString(
                                    backup_adjusted,
                                    report.adjusted(), report.adjusted()
                            ), 14, text_secondary, 4, 4
                    )
            );
        }

        column.addView(text(backup_replace_warning, 15, text_primary, 8, 12));
        var replace = new UpButton(context);
        replace.setText(backup_action_replace);
        replace.setVariant(DESTRUCTIVE);
        replace.setOnClickListener(v -> model.confirm());
        column.addView(replace, params(dp, 8));
        column.addView(cancelButton(), params(dp, 0));
    }

    private void finished(@StringRes int message, boolean error) {
        var dp = getResources().getDisplayMetrics().density;
        column.addView(text(message, 16, error ? danger : text_primary, 8, 16));
        var done = new UpButton(requireContext());
        done.setText(error ? action_close : action_done);
        done.setOnClickListener(v -> dismiss());
        column.addView(done, params(dp, 0));
        if (isAdded()) {
            if (error) Haptics.reject(done);
            else Haptics.confirm(done);
        }
    }

    // --- pieces ---

    private UpRow countRow(@StringRes int title, int count) {
        var row = new UpRow(requireContext());
        row.setTitle(getString(title));
        row.setValue(String.valueOf(count));
        return row;
    }

    private UpButton cancelButton() {
        var cancel = new UpButton(requireContext());
        cancel.setText(action_cancel);
        cancel.setVariant(SECONDARY);
        cancel.setOnClickListener(v -> dismiss());
        return cancel;
    }

    private TextView text(@StringRes int resource, float sp, int colorRes, int topDp, int bottomDp) {
        return plainText(getString(resource), sp, colorRes, topDp, bottomDp);
    }

    private TextView plainText(String value, float sp, int colorRes, int topDp, int bottomDp) {
        var dp = getResources().getDisplayMetrics().density;
        var view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(COMPLEX_UNIT_SP, sp);
        view.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        view.setPadding(Math.round(4 * dp), Math.round(topDp * dp), Math.round(4 * dp), Math.round(bottomDp * dp));
        return view;
    }

    private static LinearLayout.LayoutParams params(float dp, int bottomDp) {
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}