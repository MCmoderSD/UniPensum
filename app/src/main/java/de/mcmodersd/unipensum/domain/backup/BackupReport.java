package de.mcmodersd.unipensum.domain.backup;

public record BackupReport(
        int lecturers,
        int semesters,
        int courses,
        int series,
        int sessions,
        int skipped,
        int adjusted
) {
    public boolean isClean() {
        return skipped == 0 && adjusted == 0;
    }
}