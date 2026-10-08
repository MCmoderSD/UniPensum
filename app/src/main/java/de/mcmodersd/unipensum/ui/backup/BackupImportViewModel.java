package de.mcmodersd.unipensum.ui.backup;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.backup.BackupException;
import de.mcmodersd.unipensum.data.backup.BackupFile;
import de.mcmodersd.unipensum.data.backup.BackupManager;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.backup.BackupCleaner;
import de.mcmodersd.unipensum.domain.backup.BackupInfo;
import de.mcmodersd.unipensum.domain.backup.BackupReport;

/**
 * Where an import is, across the steps the sheet walks through. It lives as long as the sheet, so the
 * state (and the data read from the file) survives the activity being recreated.
 */
public class BackupImportViewModel extends AndroidViewModel {

    public enum Step {
        /** Reading the archive and its manifest. */
        READING,
        /** The backup is protected and needs its password (again, if {@link #wrongPassword()}). */
        PASSWORD,
        /** Decrypting and checking the data. */
        LOADING,
        /** Everything is read; waiting for the user to confirm the replacement. */
        REVIEW,
        /** Writing to the database. */
        IMPORTING,
        DONE,
        ERROR
    }

    private final MutableLiveData<Step> step = new MutableLiveData<>();
    private final BackupManager backups;
    private BackupFile.Opened opened;
    private BackupCleaner.Result loaded;
    private boolean wrongPassword;
    @StringRes
    private int errorMessage = R.string.backup_error_generic;
    private boolean started;

    public BackupImportViewModel(@NonNull Application application) {
        super(application);
        backups = UniPensumApp.from(application).backups();
    }

    public LiveData<Step> step() {
        return step;
    }

    /** What the file says about itself; only available once it has been opened. */
    @Nullable
    public BackupInfo info() {
        return opened == null ? null : opened.info();
    }

    /** The checked data and what of it can be taken over; available from {@link Step#REVIEW} on. */
    @Nullable
    public BackupReport report() {
        return loaded == null ? null : loaded.report();
    }

    public boolean wrongPassword() {
        return wrongPassword;
    }

    @StringRes
    public int errorMessage() {
        return errorMessage;
    }

    public BackupManager.Current current() {
        return backups.current();
    }

    /** Starts reading {@code source}; calling it again (after the sheet was recreated) does nothing. */
    public void start(Uri source) {
        if (started) return;
        started = true;
        step.setValue(Step.READING);
        backups.open(source, new Database.Callback<>() {
            @Override
            public void onSuccess(BackupFile.Opened result) {
                opened = result;
                if (result.isEncrypted()) step.setValue(Step.PASSWORD);
                else load(null);
            }

            @Override
            public void onError(Exception error) {
                fail(error);
            }
        });
    }

    /** Takes over the password; the array is cleared afterwards. */
    public void submitPassword(char[] password) {
        wrongPassword = false;
        load(password);
    }

    private void load(@Nullable char[] password) {
        step.setValue(Step.LOADING);
        backups.load(opened, password, new Database.Callback<>() {
            @Override
            public void onSuccess(BackupCleaner.Result result) {
                loaded = result;
                step.setValue(Step.REVIEW);
            }

            @Override
            public void onError(Exception error) {
                if (error instanceof BackupException
                        && ((BackupException) error).reason() == BackupException.Reason.WRONG_PASSWORD) {
                    wrongPassword = true;
                    step.setValue(Step.PASSWORD);
                } else {
                    fail(error);
                }
            }
        });
    }

    /** The user confirmed: replaces all data. Does nothing before {@link Step#REVIEW}. */
    public void confirm() {
        if (loaded == null || step.getValue() != Step.REVIEW) return;
        step.setValue(Step.IMPORTING);
        backups.replaceAll(loaded.data(), new Database.Callback<>() {
            @Override
            public void onSuccess(Void result) {
                step.setValue(Step.DONE);
            }

            @Override
            public void onError(Exception error) {
                // The replacement is one transaction, so the old data is still all there.
                errorMessage = R.string.backup_import_failed;
                step.setValue(Step.ERROR);
            }
        });
    }

    private void fail(Exception error) {
        errorMessage = messageFor(error);
        step.setValue(Step.ERROR);
    }

    @StringRes
    private static int messageFor(Exception error) {
        if (error instanceof BackupException) {
            switch (((BackupException) error).reason()) {
                case NOT_A_BACKUP:
                    return R.string.backup_error_not_a_backup;
                case DAMAGED:
                    return R.string.backup_error_damaged;
                case WRONG_PASSWORD:
                    return R.string.backup_error_wrong_password;
                case TOO_LARGE:
                    return R.string.backup_error_too_large;
                case UNSUPPORTED:
                    return R.string.backup_error_unsupported;
                default:
                    break;
            }
        }
        return R.string.backup_error_generic;
    }
}