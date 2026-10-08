package de.mcmodersd.unipensum.domain.model;

/** How far an edit or deletion of a session reaches within its series. */
public enum EditScope {
    THIS_ONLY,
    THIS_AND_FOLLOWING,
    ALL
}