package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.Delete;

/**
 * Access and write information whether a particular file
 * is removed on disk.
 * To be sure about the history of such changes, each time
 * a change is made, a history entry is created which is accessible
 * through {@link Delete#getDeleteHistory()}.
 */
public interface IDeleteManager {

    /**
     * Returns information whether the element
     * on the given path is deleted
     *
     * @param pathToFile The path to the element
     *
     * @return The delete information holder
     *
     * @throws InputOutputException If accessing the information fails
     */
    Delete getDelete(String pathToFile)
            throws InputOutputException;

    /**
     * Set the given path to {@link DeleteType#DELETED} and
     * add a new history entry for this change.
     *
     * @param pathToFile The path to the element which should be changed
     *
     * @throws InputOutputException If writing the new status fails
     */
    void setIsDeleted(String pathToFile)
            throws InputOutputException;

    /**
     * Set the given path to {@link DeleteType#EXISTENT} and
     * add a new history entry for this change.
     *
     * @param pathToFile The path to the element which should be changed
     *
     * @throws InputOutputException If writing the new status fails
     */
    void setIsExistent(String pathToFile)
            throws InputOutputException;
}
