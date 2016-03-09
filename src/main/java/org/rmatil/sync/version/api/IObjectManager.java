package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.api.IPathElement;
import org.rmatil.sync.persistence.core.tree.ITreeStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.Index;
import org.rmatil.sync.version.core.model.PathObject;

import java.util.List;

public interface IObjectManager {

    /**
     * Clears the index and the whole saved object history
     *
     * @throws InputOutputException
     */
    void clear()
            throws InputOutputException;

    /**s
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
     * Returns the path object for the file on the given relative path
     *
     * @param relativeFilePath The relative path of which to get the path object
     *
     * @return The path object
     *
     * @throws InputOutputException If reading the object store fails
     */
    PathObject getObjectForPath(String relativeFilePath)
            throws InputOutputException;

    /**
     * Returns the corresponding hash for the given file path
     *
     * @param relativeFilePath The file path for which to get the hash
     *
     * @return The hash of the specified path
     */
    String getHashForPath(String relativeFilePath);

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

    /**
     * Returns the object directory
     *
     * @return The object directory path
     */
    IPathElement getObjectDir();

    /**
     * Returns the storage adapter to access the object store manually.
     * The returned storage adapter will have the the object store as root path
     *
     * @return The storage adapter for the object store
     */
    ITreeStorageAdapter getStorageAdapater();
}
