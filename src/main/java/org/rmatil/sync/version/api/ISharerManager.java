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
     * @param username   The sharer's username to add
     * @param accessType The access type which is granted to the sharer
     * @param pathToFile The path to the file
     *
     * @throws InputOutputException If adding the sharer failed
     */
    void addSharer(String username, AccessType accessType, String pathToFile)
            throws InputOutputException;

    /**
     * Removes the given sharer from the file on the specified path
     *
     * @param username   The username of the sharer to remove
     * @param pathToFile The file path from which to remove the sharer
     *
     * @throws InputOutputException If removing the sharer failed
     */
    void removeSharer(String username, String pathToFile)
            throws InputOutputException;

    /**
     * Adds the owner to the file on the specified path
     *
     * @param username   The owner's user name to add
     * @param pathToFile The file path to which to add the owner
     *
     * @throws InputOutputException If adding the owner fails
     */
    void addOwner(String username, String pathToFile)
            throws InputOutputException;

    /**
     * Removes the owner of the file specified
     *
     * @param pathToFile The path to the file from which to remove the owner
     *
     * @throws InputOutputException If removing the owner fails
     */
    void removeOwner(String pathToFile)
            throws InputOutputException;

    /**
     * Returns the owner of the file specified
     *
     * @param pathToFile The path to the file from which to get the owner
     *
     * @return The owner's username or null if no owner is registered
     *
     * @throws InputOutputException If accessing the object store fails
     */
    String getOwner(String pathToFile)
            throws InputOutputException;

    /**
     * Returns the used object manager
     *
     * @return The object manager used to alter sharers
     */
    IObjectManager getObjectManager();
}
