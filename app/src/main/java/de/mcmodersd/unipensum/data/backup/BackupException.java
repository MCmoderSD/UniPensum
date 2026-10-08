package de.mcmodersd.unipensum.data.backup;

/** A backup file that could not be read, with the reason the UI turns into a message. */
public final class BackupException extends Exception {

    public enum Reason {
        /** Not a UniPensum backup: no zip archive, or one without the manifest. */
        NOT_A_BACKUP,
        /** Looks like a backup, but its content does not hold together. */
        DAMAGED,
        /** The password is missing or wrong, or the file was changed after it was written. */
        WRONG_PASSWORD,
        /** The file, or what it unpacks to, is larger than any real backup. */
        TOO_LARGE,
        /** Written by a newer version with a way of protecting the data this one does not know. */
        UNSUPPORTED
    }

    private final Reason reason;

    public BackupException(Reason reason) {
        this(reason, null);
    }

    public BackupException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}