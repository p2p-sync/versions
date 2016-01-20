package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.ObjectStore;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface IObjectStore {

    /**
     * Syncs the index content with the
     * index in the object store
     *
     * @param rootSyncDir The absolute path to the synchronized directory root
     *
     * @throws InputOutputException If accessing the filesystem fails somewhere
     */
    void sync(File rootSyncDir)
            throws InputOutputException;

	/**
	 * Syncs the index content with the index in the object store
	 *
	 * @param rootSyncDir The absolute path to the synchronized directory root
	 * @param ignoredFiles A list of ignored files (relative to the root directory of the object store)
	 *
	 * @throws InputOutputException If accessing the filesystem fails somewhere
	 */
	void sync(File rootSyncDir, List<String> ignoredFiles)
			throws InputOutputException;

    /**
     * This method should be invoked when a new file is created to store
     * a new path object for it in the object store
     *
     * @param relativePath The relative path to the root dir of the created file
     * @param contentHash  The hash of the created file's content
     *
     * @throws InputOutputException If writing to the object stored failed
     */
    void onCreateFile(String relativePath, String contentHash)
            throws InputOutputException;

    /**
     * This method should be invoked when a file is updated to also update
     * the list of versions in the object store
     *
     * @param relativePath The relative path to the root dir of the modified file
     * @param contentHash  The new hash of the file
     *
     * @throws InputOutputException Thrown if adding the new version failed
     */
    void onModifyFile(String relativePath, String contentHash)
            throws InputOutputException;

    /**
     * This method should be called when a file is deleted from disk to remove
     * all corresponding objects also from the object store
     *
     * @param relativePath The relative path to the root dir of the deleted object
     *
     * @throws InputOutputException
     */
    void onRemoveFile(String relativePath)
            throws InputOutputException;

    /**
     * This method should be called when a file is moved to also move the corresponding
     * objects within the object stored
     *
     * @param oldRelativePath The path on which the file was stored before moving (relative to the root dir)
     * @param newRelativePath The path on which the file is newly stored (relative to the root dir)
     *
     * @throws InputOutputException If moving the path object fails
     */
    void onMoveFile(String oldRelativePath, String newRelativePath)
            throws InputOutputException;

    /**
     * Merges the given object store into this one. If files are missing
     * or an outdated version is detected, then its path is contained in the returned
     * list. All such paths have then to be refetched from the client of which the given
     * object store is fetched from.
     *
     * @param otherObjectStore The object store to merge into this one
     *
     * @return A list of relative paths which are either outdated or missing on the file system
     *
     * @throws InputOutputException If reading the other object store fails
     */
    HashMap<ObjectStore.MergedObjectType, Set<String>> mergeObjectStore(IObjectStore otherObjectStore)
            throws InputOutputException;

    /**
     * Returns the used instance for object store modification
     *
     * @return The object manager
     */
    IObjectManager getObjectManager();
}
