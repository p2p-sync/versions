package org.rmatil.sync.version.api;

/**
 * The access types for a sharer.
 * <p>
 * Note, that the order of the values
 * is important, since they build an implicit
 * chain of access: Every sharer having write access,
 * has also access to read. However, a sharer having only read
 * access does not have write access.
 * <code>Enum.values()</code> can be used to get the values
 * in declared order
 */
public enum AccessType {

    /**
     * If a sharer lost his read/write access
     */
    ACCESS_REMOVED,

    /**
     * If a sharer has read access
     */
    READ,

    /**
     * If a sharer should have write access
     */
    WRITE
}
