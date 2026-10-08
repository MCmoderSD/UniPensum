package de.mcmodersd.unipensum.domain.logic;

import java.util.ArrayList;

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
    public record NewSeries(Series series, ArrayList<Session> sessions, ArrayList<Session> adopted) { }

    public final ArrayList<NewSeries> newSeries = new ArrayList<>();
    public final ArrayList<Series> updatedSeries = new ArrayList<>();
    public final ArrayList<Long> deletedSeriesIds = new ArrayList<>();
    /** Sessions inserted into already existing series. */
    public final ArrayList<Session> newSessions = new ArrayList<>();
    public final ArrayList<Session> updatedSessions = new ArrayList<>();
    public final ArrayList<Long> deletedSessionIds = new ArrayList<>();

    public boolean isEmpty() {
        return newSeries.isEmpty()
                && updatedSeries.isEmpty()
                && deletedSeriesIds.isEmpty()
                && newSessions.isEmpty()
                && updatedSessions.isEmpty()
                && deletedSessionIds.isEmpty();
    }
}