package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;

import java.util.Set;

/**
 * This interface provides access to a {@link PathObject}'s sharer.
 */
public interface ISharerManager {

    /**
     * Returns a set of sharers of the given path
     *
     * @param pathToFile The path of which to get all sharers
     *
     * @return Returns the set of sharers for the given path
     *
     * @throws InputOutputException If reading sharers from the object store failed
     */
    Set<Sharer> getSharer(String pathToFile)
            throws InputOutputException;

    /**
     * Adds the given sharer for the file on the given path
     *
     * @param sharer     The sharer to add
     * @param pathToFile The path to the file
     *
     * @throws InputOutputException If adding the sharer failed
     */
    void addSharer(Sharer sharer, String pathToFile)
            throws InputOutputException;

    /**
     * Removes the given sharer from the file on the specified path
     *
     * @param sharer     The sharer to remove
     * @param pathToFile The file path from which to remove the sharer
     *
     * @throws InputOutputException If removing the sharer failed
     */
    void removeSharer(Sharer sharer, String pathToFile)
            throws InputOutputException;

    /**
     * Returns the used object manager
     *
     * @return The object manager used to alter sharers
     */
    IObjectManager getObjectManager();
}
