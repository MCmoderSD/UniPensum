package de.mcmodersd.unipensum.domain.logic;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.Session;

/**
 * The writes an edit boils down to. Produced by {@link SeriesEditor} and {@link SemesterRules}
 * without touching a database, applied by the repository in a single transaction.
 * The lists are filled by the producers and are read-only for everyone else.
 * Deleting a series cascades to its sessions, so those are not listed again.
 */
public final class ChangeSet {

    /**
     * A series that does not exist yet.
     *
     * @param sessions new sessions to insert into it (their {@code seriesId} is ignored)
     * @param adopted  existing sessions that move into it; their {@code seriesId} is overwritten
     */
    public record NewSeries(Series series, List<Session> sessions, List<Session> adopted) {
    }

    public final List<NewSeries> newSeries = new ArrayList<>();
    public final List<Series> updatedSeries = new ArrayList<>();
    public final List<Long> deletedSeriesIds = new ArrayList<>();
    /** Sessions inserted into already existing series. */
    public final List<Session> newSessions = new ArrayList<>();
    public final List<Session> updatedSessions = new ArrayList<>();
    public final List<Long> deletedSessionIds = new ArrayList<>();

    public boolean isEmpty() {
        return newSeries.isEmpty()
                && updatedSeries.isEmpty()
                && deletedSeriesIds.isEmpty()
                && newSessions.isEmpty()
                && updatedSessions.isEmpty()
                && deletedSessionIds.isEmpty();
    }
}