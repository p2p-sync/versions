package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.Index;
import org.rmatil.sync.version.core.model.PathObject;

import java.util.List;

public interface IObjectManager {

    /**
     * Writes the given PathObject to the object store
     *
     * @param path The path object to write
     *
     * @throws InputOutputException If writing fails
     */
    void writeObject(PathObject path)
            throws InputOutputException;

    /**
     * Returns the path object for the given filename hash, if any
     *
     * @param fileNameHash The file name hash of which to get the path object
     *
     * @return The found path object
     *
     * @throws InputOutputException If reading the object store fails
     */
    PathObject getObject(String fileNameHash)
            throws InputOutputException;

    /**
     * Removes the path object of the given file name hash from the object store
     *
     * @param fileNameHash The hash of the file name of which the path object should be removed
     *
     * @throws InputOutputException If removing fails
     */
    void removeObject(String fileNameHash)
            throws InputOutputException;

    /**
     * Returns all path objects which are children of the given parent file
     *
     * @param relativeParentFileName The relative path of the parent of which to get the children
     *
     * @return The list of children
     *
     * @throws InputOutputException If reading the object store fails
     */
    List<PathObject> getChildren(String relativeParentFileName)
    throws InputOutputException;

    /**
     * Returns the current instance of the object store's index
     *
     * @return The index
     */
    Index getIndex();

    /**
     * Returns the file name in which the index is stored
     *
     * @return The file name
     */
    String getIndexFileName();

}