package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.Version;

import java.util.List;

/**
 * Handles access to versions of a certain file
 */
public interface IVersionManager {

    /**
     * Returns the versions of the given file
     *
     * @param pathToFile The path to the file for which the versions should be returned
     *
     * @return A list of versions
     *
     * @throws InputOutputException If reading versions failed
     */
    List<Version> getVersions(String pathToFile)
            throws InputOutputException;

    /**
     * Adds the given version to the object store of the specified file
     *
     * @param version    The version to add
     * @param pathToFile The path to the file
     *
     * @throws InputOutputException If reading and modifying of the object store fails
     */
    void addVersion(Version version, String pathToFile)
            throws InputOutputException;

    /**
     * Removes the given version from the object store of the specified file
     *
     * @param version    The version to remove
     * @param pathToFile The path to the file
     *
     * @throws InputOutputException If reading and removing the version from the object store fails
     */
    void removeVersion(Version version, String pathToFile)
            throws InputOutputException;

    /**
     * Returns the object manager which manages the versions
     *
     * @return The object manager
     */
    IObjectManager getObjectManager();

}
